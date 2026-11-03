package tech.illuin.wombat.core.activity.llm;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.illuin.wombat.core.activity.WombatActivityException;
import tech.illuin.wombat.core.activity.commons.AssetActivity;
import tech.illuin.wombat.core.activity.commons.AssetFilter;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.evaluation.impact.llm.MicrometerLLMMetricResolver;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static tech.illuin.wombat.core.source.persistence.micrometer.MicrometerTags.*;

@ExtendWith(MockitoExtension.class)
class LLMActivityResolverMicrometerTest
{
    private MeterRegistry registry;
    private MicrometerLLMMetricResolver metricResolver;
    private LLMActivityResolver resolver;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Asset asset;

    @Mock
    private AssetFilter filter;

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-01-01T02:00:00Z");
    private static final Duration RESOLUTION = Duration.ofHours(1);
    private static final String ASSET_ID = "llm-asset-1";

    @BeforeEach
    void setUp()
    {
        this.registry = new SimpleMeterRegistry();
        this.metricResolver = new MicrometerLLMMetricResolver(this.registry);
        this.resolver = new LLMActivityResolver(this.metricResolver);
        lenient().when(this.asset.type().regime()).thenReturn(ActivityRegime.MODELED);
    }

    @Test
    void accept_whenLLMFamily_returnsTrue()
    {
        when(this.asset.type().family()).thenReturn(ServiceFamily.LLM);
        assertTrue(this.resolver.accept(this.asset));
    }

    @Test
    void accept_whenNotLLMFamily_returnsFalse()
    {
        when(this.asset.type().family()).thenReturn(null);
        assertFalse(this.resolver.accept(this.asset));

        Arrays.stream(ServiceFamily.values())
            .filter(family -> family != ServiceFamily.LLM)
            .findFirst()
            .ifPresent(otherFamily -> {
                when(this.asset.type().family()).thenReturn(otherFamily);
                assertFalse(this.resolver.accept(this.asset));
            });
    }

    @Test
    void resolve_whenRegistryEmpty_returnsEmpty() throws WombatActivityException
    {
        when(this.asset.identity().id()).thenReturn(ASSET_ID);
        TimeRange range = new TimeRange(START, END, RESOLUTION);

        Optional<AssetActivity> result = this.resolver.resolve(this.asset, range, this.filter);
        assertTrue(result.isEmpty());
    }

    @Test
    void resolve_whenTargetAssetHasNoMetrics_returnsEmpty() throws WombatActivityException
    {
        recordLLMMetric("other-asset", "service-1", LLMProvider.openai, "gpt-4o", "USA", 500, 10);
        when(this.asset.identity().id()).thenReturn(ASSET_ID);
        TimeRange range = new TimeRange(START, END, RESOLUTION);

        Optional<AssetActivity> result = this.resolver.resolve(this.asset, range, this.filter);
        assertTrue(result.isEmpty());
    }

    @Test
    void resolve_withMultipleSlots_resolvesActivityDataPerSlot() throws WombatActivityException
    {
        recordLLMMetric(ASSET_ID, "mistral-service", LLMProvider.mistralai, "mistral-large", "FRA", 500, 10);
        recordLLMMetric(ASSET_ID, "openai-service", LLMProvider.openai, "gpt-4o", "USA", 1500, 30);

        when(this.asset.identity().id()).thenReturn(ASSET_ID);
        when(this.filter.filterServiceIds(this.asset)).thenReturn(Set.of("mistral-service", "openai-service"));

        TimeRange range = new TimeRange(START, END, RESOLUTION); // 2 slots of 1 hour
        Instant mid = START.plus(RESOLUTION);

        Optional<AssetActivity> result = this.resolver.resolve(this.asset, range, this.filter);

        assertTrue(result.isPresent());
        AssetActivity assetActivity = result.get();
        assertEquals(this.asset.identity(), assetActivity.assetIdentity());
        assertEquals(2, assetActivity.activity().size());

        // Validate Slot 1
        LLMActivityData data1 = (LLMActivityData) assetActivity.activity().get(0);
        assertEquals(START, data1.range().start());
        assertEquals(mid, data1.range().end());
        assertEquals(ActivityRegime.MODELED, data1.regime());
        assertEquals(Set.of("mistral-service", "openai-service"), data1.serviceIds());
        assertEquals(2, data1.services().size());
        assertEquals(2000L, data1.outputTokenCount());
        assertEquals(40.0, data1.requestCount());

        LLMServiceActivity mistral1 = data1.services().get("mistral-service");
        assertEquals(LLMProvider.mistralai, mistral1.provider());
        assertEquals("mistral-large", mistral1.model());
        assertEquals(Country.FRANCE, mistral1.location());
        assertEquals(500L, mistral1.outputTokenCount());
        assertEquals(10.0, mistral1.requestCount());

        LLMServiceActivity openai1 = data1.services().get("openai-service");
        assertEquals(LLMProvider.openai, openai1.provider());
        assertEquals("gpt-4o", openai1.model());
        assertEquals(Country.US, openai1.location());
        assertEquals(1500L, openai1.outputTokenCount());
        assertEquals(30.0, openai1.requestCount());

        // Validate Slot 2
        LLMActivityData data2 = (LLMActivityData) assetActivity.activity().get(1);
        assertEquals(mid, data2.range().start());
        assertEquals(END, data2.range().end());
        assertEquals(ActivityRegime.MODELED, data2.regime());
        assertEquals(Set.of("mistral-service", "openai-service"), data2.serviceIds());
        assertEquals(2, data2.services().size());
        assertEquals(2000L, data2.outputTokenCount());
        assertEquals(40.0, data2.requestCount());
    }

    @Test
    void resolve_whenFilterIsNull_defaultsToAllActivitiesAndUsesKeysAsServiceIds() throws WombatActivityException
    {
        recordLLMMetric(ASSET_ID, "svc-1", LLMProvider.mistralai, "mistral-large", "FRA", 500, 10);
        recordLLMMetric(ASSET_ID, "svc-2", LLMProvider.openai, "gpt-4o", "USA", 1500, 30);

        when(this.asset.identity().id()).thenReturn(ASSET_ID);
        TimeRange range = new TimeRange(START, START.plus(RESOLUTION), RESOLUTION);

        Optional<AssetActivity> result = this.resolver.resolve(this.asset, range, null);

        assertTrue(result.isPresent());
        LLMActivityData data = (LLMActivityData) result.get().activity().getFirst();

        assertEquals(Set.of("svc-1", "svc-2"), data.serviceIds());
        assertEquals(2, data.services().size());
        assertEquals(2000L, data.outputTokenCount());
        assertEquals(40.0, data.requestCount());
    }

    @Test
    void resolve_whenFilterMatchesKeyOrModel_filtersCorrectly() throws WombatActivityException
    {
        recordLLMMetric(ASSET_ID, "matched-by-key", LLMProvider.mistralai, "other-model", "FRA", 100, 2);
        recordLLMMetric(ASSET_ID, "arbitrary-key", LLMProvider.openai, "gpt-4o", "USA", 200, 4);
        recordLLMMetric(ASSET_ID, "unrelated-key", LLMProvider.mistralai, "unrelated-model", "FRA", 300, 6);

        when(this.asset.identity().id()).thenReturn(ASSET_ID);
        when(this.filter.filterServiceIds(this.asset)).thenReturn(Set.of("matched-by-key", "gpt-4o"));

        TimeRange range = new TimeRange(START, START.plus(RESOLUTION), RESOLUTION);

        Optional<AssetActivity> result = this.resolver.resolve(this.asset, range, this.filter);

        assertTrue(result.isPresent());
        LLMActivityData data = (LLMActivityData) result.get().activity().getFirst();

        assertEquals(Set.of("matched-by-key", "gpt-4o"), data.serviceIds());
        assertEquals(2, data.services().size());
        assertTrue(data.services().containsKey("matched-by-key"));
        assertTrue(data.services().containsKey("arbitrary-key"));
        assertFalse(data.services().containsKey("unrelated-key"));
        assertEquals(300L, data.outputTokenCount()); // 100 + 200
        assertEquals(6.0, data.requestCount()); // 2.0 + 4.0
    }

    @Test
    void resolve_whenFilterExcludesAllActivities_returnsEmpty() throws WombatActivityException
    {
        recordLLMMetric(ASSET_ID, "svc-1", LLMProvider.mistralai, "mistral-large", "FRA", 500, 10);

        when(this.asset.identity().id()).thenReturn(ASSET_ID);
        when(this.filter.filterServiceIds(this.asset)).thenReturn(Set.of("non-existent-filter"));

        TimeRange range = new TimeRange(START, START.plus(RESOLUTION), RESOLUTION);

        Optional<AssetActivity> result = this.resolver.resolve(this.asset, range, this.filter);
        assertTrue(result.isEmpty());
    }

    @Test
    void resolve_withMultipleAssetsInRegistry_isolatesTargetAssetActivities() throws WombatActivityException
    {
        recordLLMMetric(ASSET_ID, "svc-1", LLMProvider.mistralai, "mistral-large", "FRA", 500, 10);
        recordLLMMetric("other-asset", "svc-2", LLMProvider.openai, "gpt-4o", "USA", 1500, 30);

        when(this.asset.identity().id()).thenReturn(ASSET_ID);
        TimeRange range = new TimeRange(START, START.plus(RESOLUTION), RESOLUTION);

        Optional<AssetActivity> result = this.resolver.resolve(this.asset, range, null);

        assertTrue(result.isPresent());
        LLMActivityData data = (LLMActivityData) result.get().activity().getFirst();
        assertEquals(Set.of("svc-1"), data.serviceIds());
        assertEquals(1, data.services().size());
        assertEquals(500L, data.outputTokenCount());
        assertEquals(10.0, data.requestCount());
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
