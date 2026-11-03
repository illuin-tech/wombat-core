package tech.illuin.wombat.core.evaluation.impact.llm;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.activity.llm.LLMServiceActivity;
import tech.illuin.wombat.core.asset.profile.LLMProvider;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.illuin.wombat.core.source.persistence.micrometer.MicrometerTags.*;

class MicrometerLLMMetricResolverTest
{
    private MeterRegistry registry;
    private MicrometerLLMMetricResolver resolver;

    @BeforeEach
    void setUp()
    {
        this.registry = new SimpleMeterRegistry();
        this.resolver = new MicrometerLLMMetricResolver(this.registry);
    }

    @Test
    void sumOutputTokens_aggregatesTokensForAsset()
    {
        recordLLMMetric("asset-1", "service-1", LLMProvider.mistralai, "mistral-large", "FRA", 100, 2);
        recordLLMMetric("asset-1", "service-2", LLMProvider.openai, "gpt-4o", "USA", 250, 5);
        recordLLMMetric("asset-2", "service-3", LLMProvider.anthropic, "claude-3-5-sonnet", "USA", 500, 10);

        long asset1Tokens = this.resolver.sumOutputTokens(0, 3600_000, "asset-1");
        assertEquals(350L, asset1Tokens);

        long asset2Tokens = this.resolver.sumOutputTokens(0, 3600_000, "asset-2");
        assertEquals(500L, asset2Tokens);

        long unknownTokens = this.resolver.sumOutputTokens(0, 3600_000, "unknown");
        assertEquals(0L, unknownTokens);
    }

    @Test
    void sumOutputTokens_withNullAssetId_sumsAllAssets()
    {
        recordLLMMetric("asset-1", "service-1", LLMProvider.mistralai, "mistral-large", "FRA", 100, 2);
        recordLLMMetric("asset-2", "service-2", LLMProvider.openai, "gpt-4o", "USA", 250, 5);

        long allTokens = this.resolver.sumOutputTokens(0, 3600_000, null);
        assertEquals(350L, allTokens);
    }

    @Test
    void sumOutputTokens_emptyRegistry_returnsZero()
    {
        assertEquals(0L, this.resolver.sumOutputTokens(0, 3600_000, "asset-1"));
    }

    @Test
    void sumOutputTokens_withOnlyRequestCounts_returnsZero()
    {
        Tags tags = Tags.of(
            TAG_ENVIRONMENT, "env-1",
            TAG_ASSET, "asset-1",
            TAG_SERVICE, "service-1"
        );
        DistributionSummary.builder(METRIC_LLM_REQUEST_COUNT)
            .tags(tags)
            .register(this.registry)
            .record(10.0);

        assertEquals(0L, this.resolver.sumOutputTokens(0, 3600_000, "asset-1"));
    }

    @Test
    void serviceActivities_mapsAndAggregatesServiceActivities()
    {
        recordLLMMetric("asset-1", "service-1", LLMProvider.mistralai, "mistral-large", "FRA", 100, 2);
        recordLLMMetric("asset-1", "service-2", LLMProvider.openai, "gpt-4o", "USA", 250, 5);

        Map<String, LLMServiceActivity> activities = this.resolver.serviceActivities(0, 3600_000, "asset-1");
        assertEquals(2, activities.size());

        LLMServiceActivity activity1 = activities.get("service-1");
        assertEquals(LLMProvider.mistralai, activity1.provider());
        assertEquals("mistral-large", activity1.model());
        assertEquals(Country.FRANCE, activity1.location());
        assertEquals(100L, activity1.outputTokenCount());
        assertEquals(2.0, activity1.requestCount());

        LLMServiceActivity activity2 = activities.get("service-2");
        assertEquals(LLMProvider.openai, activity2.provider());
        assertEquals("gpt-4o", activity2.model());
        assertEquals(Country.US, activity2.location());
        assertEquals(250L, activity2.outputTokenCount());
        assertEquals(5.0, activity2.requestCount());
    }

    @Test
    void serviceActivities_withMissingServiceTag_ignoresGroup()
    {
        Tags tagsWithoutService = Tags.of(
            TAG_ENVIRONMENT, "env-1",
            TAG_ASSET, "asset-1",
            TAG_ASSET_NAME, "asset-1",
            TAG_ASSET_TYPE, "llm",
            TAG_LLM_PROVIDER, LLMProvider.openai.name(),
            TAG_LLM_MODEL, "gpt-4o",
            TAG_LLM_LOCATION, "USA"
        );
        DistributionSummary.builder(METRIC_LLM_OUTPUT_TOKENS).tags(tagsWithoutService).register(this.registry).record(100.0);

        Map<String, LLMServiceActivity> activities = this.resolver.serviceActivities(0, 3600_000, "asset-1");
        assertTrue(activities.isEmpty());
    }

    @Test
    void serviceActivities_withInvalidCountryCode_handlesNullLocation()
    {
        Tags tags = Tags.of(
            TAG_ENVIRONMENT, "env-1",
            TAG_ASSET, "asset-1",
            TAG_ASSET_NAME, "asset-1",
            TAG_ASSET_TYPE, "llm",
            TAG_SERVICE, "service-1",
            TAG_LLM_PROVIDER, LLMProvider.openai.name(),
            TAG_LLM_MODEL, "gpt-4o",
            TAG_LLM_LOCATION, "UNKNOWN_CODE"
        );
        DistributionSummary.builder(METRIC_LLM_OUTPUT_TOKENS).tags(tags).register(this.registry).record(150.0);
        DistributionSummary.builder(METRIC_LLM_REQUEST_COUNT).tags(tags).register(this.registry).record(3.0);

        Map<String, LLMServiceActivity> activities = this.resolver.serviceActivities(0, 3600_000, "asset-1");
        assertEquals(1, activities.size());

        LLMServiceActivity activity = activities.get("service-1");
        assertEquals(LLMProvider.openai, activity.provider());
        assertEquals("gpt-4o", activity.model());
        org.junit.jupiter.api.Assertions.assertNull(activity.location());
        assertEquals(150L, activity.outputTokenCount());
        assertEquals(3.0, activity.requestCount());
    }

    @Test
    void serviceActivities_mergesMultipleRecordsForSameService()
    {
        recordLLMMetric("asset-1", "service-1", LLMProvider.openai, "gpt-4o", "USA", 100, 2);
        recordLLMMetric("asset-1", "service-1", LLMProvider.openai, "gpt-4o", "USA", 150, 3);

        Map<String, LLMServiceActivity> activities = this.resolver.serviceActivities(0, 3600_000, "asset-1");
        assertEquals(1, activities.size());

        LLMServiceActivity activity = activities.get("service-1");
        assertEquals(LLMProvider.openai, activity.provider());
        assertEquals("gpt-4o", activity.model());
        assertEquals(Country.US, activity.location());
        assertEquals(250L, activity.outputTokenCount());
        assertEquals(5.0, activity.requestCount());
    }

    @Test
    void serviceActivities_emptyRegistryOrUnknownAsset_returnsEmpty()
    {
        recordLLMMetric("asset-1", "service-1", LLMProvider.openai, "gpt-4o", "USA", 100, 2);

        assertTrue(this.resolver.serviceActivities(0, 3600_000, "unknown").isEmpty());
    }

    @Test
    void outputTokensPerBucket_calculatesTokensPerBucketWithFiltering()
    {
        recordLLMMetric("asset-1", "service-1", LLMProvider.mistralai, "mistral-large", "FRA", 100, 2);
        recordLLMMetric("asset-1", "service-2", LLMProvider.openai, "gpt-4o", "USA", 250, 5);

        long startMs = 1000;
        long endMs = 4000;
        long stepMs = 3600_000;
        long expectedBucket = 0;

        Map<Long, Double> tokensAll = this.resolver.outputTokensPerBucket(startMs, endMs, stepMs, "asset-1", List.of());
        assertEquals(Map.of(expectedBucket, 350.0), tokensAll);

        Map<Long, Double> tokensFilteredByService = this.resolver.outputTokensPerBucket(startMs, endMs, stepMs, "asset-1", List.of("service-1"));
        assertEquals(Map.of(expectedBucket, 100.0), tokensFilteredByService);

        Map<Long, Double> tokensFilteredByModel = this.resolver.outputTokensPerBucket(startMs, endMs, stepMs, "asset-1", List.of("gpt-4o"));
        assertEquals(Map.of(expectedBucket, 250.0), tokensFilteredByModel);
    }

    @Test
    void outputTokensPerBucket_withZeroTokens_returnsEmpty()
    {
        recordLLMMetric("asset-1", "service-1", LLMProvider.openai, "gpt-4o", "USA", 0.0, 2);

        Map<Long, Double> emptyResult = this.resolver.outputTokensPerBucket(1000, 4000, 3600_000, "asset-1", List.of());
        assertTrue(emptyResult.isEmpty());
    }

    @Test
    void outputTokensPerBucket_withNonMatchingFilters_returnsEmpty()
    {
        recordLLMMetric("asset-1", "service-1", LLMProvider.openai, "gpt-4o", "USA", 100, 2);

        Map<Long, Double> nonMatchingAsset = this.resolver.outputTokensPerBucket(1000, 4000, 3600_000, "unknown", List.of());
        assertTrue(nonMatchingAsset.isEmpty());

        Map<Long, Double> nonMatchingService = this.resolver.outputTokensPerBucket(1000, 4000, 3600_000, "asset-1", List.of("unknown-service"));
        assertTrue(nonMatchingService.isEmpty());
    }

    @Test
    void outputTokensPerBucket_alignsBucketToStep()
    {
        recordLLMMetric("asset-1", "service-1", LLMProvider.openai, "gpt-4o", "USA", 100, 2);

        long startMs = 7200_000 + 1500;
        long endMs = startMs + 1000;
        long stepMs = 3600_000;
        long expectedBucket = 7200_000;

        Map<Long, Double> result = this.resolver.outputTokensPerBucket(startMs, endMs, stepMs, "asset-1", List.of());
        assertEquals(Map.of(expectedBucket, 100.0), result);
    }

    private void recordLLMMetric(String assetId, String serviceId, LLMProvider provider, String model, String countryCode, double tokens, double requests)
    {
        Tags tags = Tags.of(
            TAG_ENVIRONMENT, "env-1",
            TAG_ASSET, assetId,
            TAG_ASSET_NAME, assetId,
            TAG_ASSET_TYPE, "llm",
            TAG_SERVICE, serviceId,
            TAG_LLM_PROVIDER, provider.name(),
            TAG_LLM_MODEL, model,
            TAG_LLM_LOCATION, countryCode
        );

        DistributionSummary.builder(METRIC_LLM_OUTPUT_TOKENS)
            .tags(tags)
            .register(this.registry)
            .record(tokens);

        DistributionSummary.builder(METRIC_LLM_REQUEST_COUNT)
            .tags(tags)
            .register(this.registry)
            .record(requests);
    }
}
