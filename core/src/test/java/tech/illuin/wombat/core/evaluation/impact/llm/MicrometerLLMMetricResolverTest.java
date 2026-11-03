package tech.illuin.wombat.core.evaluation.impact.llm;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.activity.llm.LLMServiceActivity;
import tech.illuin.wombat.core.asset.profile.LLMProvider;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.illuin.wombat.core.source.persistence.micrometer.MicrometerTags.*;

class MicrometerLLMMetricResolverTest
{
    private MeterRegistry registry;
    private MicrometerLLMMetricResolver resolver;

    private static final TimeRange HOUR = new TimeRange(Instant.EPOCH, Instant.EPOCH.plusSeconds(3600));

    @BeforeEach
    void setUp()
    {
        this.registry = new SimpleMeterRegistry();
        this.resolver = new MicrometerLLMMetricResolver(this.registry);
    }

    @Test
    void serviceActivitiesPerBucket_mapsAndAggregatesServiceActivities()
    {
        recordLLMMetric("asset-1", "service-1", LLMProvider.mistralai, "mistral-large", "FRA", 100, 2);
        recordLLMMetric("asset-1", "service-2", LLMProvider.openai, "gpt-4o", "USA", 250, 5);

        Map<String, LLMServiceActivity> activities = this.resolver.serviceActivitiesPerBucket(HOUR, "asset-1").getOrDefault(0L, Map.of());
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
    void serviceActivitiesPerBucket_withMissingServiceTag_ignoresGroup()
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

        Map<String, LLMServiceActivity> activities = this.resolver.serviceActivitiesPerBucket(HOUR, "asset-1").getOrDefault(0L, Map.of());
        assertTrue(activities.isEmpty());
    }

    @Test
    void serviceActivitiesPerBucket_withInvalidCountryCode_handlesNullLocation()
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

        Map<String, LLMServiceActivity> activities = this.resolver.serviceActivitiesPerBucket(HOUR, "asset-1").getOrDefault(0L, Map.of());
        assertEquals(1, activities.size());

        LLMServiceActivity activity = activities.get("service-1");
        assertEquals(LLMProvider.openai, activity.provider());
        assertEquals("gpt-4o", activity.model());
        org.junit.jupiter.api.Assertions.assertNull(activity.location());
        assertEquals(150L, activity.outputTokenCount());
        assertEquals(3.0, activity.requestCount());
    }

    @Test
    void serviceActivitiesPerBucket_mergesMultipleRecordsForSameService()
    {
        recordLLMMetric("asset-1", "service-1", LLMProvider.openai, "gpt-4o", "USA", 100, 2);
        recordLLMMetric("asset-1", "service-1", LLMProvider.openai, "gpt-4o", "USA", 150, 3);

        Map<String, LLMServiceActivity> activities = this.resolver.serviceActivitiesPerBucket(HOUR, "asset-1").getOrDefault(0L, Map.of());
        assertEquals(1, activities.size());

        LLMServiceActivity activity = activities.get("service-1");
        assertEquals(LLMProvider.openai, activity.provider());
        assertEquals("gpt-4o", activity.model());
        assertEquals(Country.US, activity.location());
        assertEquals(250L, activity.outputTokenCount());
        assertEquals(5.0, activity.requestCount());
    }

    @Test
    void serviceActivitiesPerBucket_emptyRegistryOrUnknownAsset_returnsEmpty()
    {
        recordLLMMetric("asset-1", "service-1", LLMProvider.openai, "gpt-4o", "USA", 100, 2);

        assertTrue(this.resolver.serviceActivitiesPerBucket(HOUR, "unknown").getOrDefault(0L, Map.of()).isEmpty());
    }

    /** The registry holds a snapshot rather than a history, so every slot of the range reads the same values. */
    @Test
    void serviceActivitiesPerBucket_repeatsTheSnapshotForEverySlot()
    {
        recordLLMMetric("asset-1", "service-1", LLMProvider.openai, "gpt-4o", "FRA", 100.0, 2.0);
        TimeRange range = new TimeRange(Instant.EPOCH, Instant.EPOCH.plusSeconds(2 * 3600), Duration.ofHours(1));

        Map<Long, Map<String, LLMServiceActivity>> perSlot = this.resolver.serviceActivitiesPerBucket(range, "asset-1");

        assertEquals(Set.of(0L, 3600_000L), perSlot.keySet());
        assertEquals(100L, perSlot.get(3600_000L).get("service-1").outputTokenCount());
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
