package tech.illuin.wombat.core.activity.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.illuin.wombat.core.activity.WombatActivityException;
import tech.illuin.wombat.core.activity.commons.AssetActivity;
import tech.illuin.wombat.core.activity.commons.AssetFilter;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.evaluation.impact.llm.LLMMetricResolver;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static tech.illuin.wombat.core.activity.commons.Country.FRANCE;
import static tech.illuin.wombat.core.activity.commons.TimeRange.toEpochMs;

@ExtendWith(MockitoExtension.class)
class LLMActivityResolverTest
{
    @Mock
    private LLMMetricResolver metricResolver;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Asset asset;

    @Mock
    private AssetFilter filter;

    private LLMActivityResolver resolver;

    private final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private final Instant END = Instant.parse("2026-01-01T02:00:00Z");
    private final Duration RESOLUTION = Duration.ofHours(1);
    private final String ASSET_ID = "llm-asset-1";

    @BeforeEach
    void setUp()
    {
        resolver = new LLMActivityResolver(metricResolver);
        lenient().when(asset.type().regime()).thenReturn(ActivityRegime.MODELED);
    }

    @Test
    void accept_whenLLMFamily_returnsTrue()
    {
        when(asset.type().family()).thenReturn(ServiceFamily.LLM);

        assertTrue(resolver.accept(asset));
    }

    @Test
    void accept_whenNotLLMFamily_returnsFalse()
    {
        when(asset.type().family()).thenReturn(null);
        assertFalse(resolver.accept(asset));

        Arrays.stream(ServiceFamily.values())
            .filter(family -> family != ServiceFamily.LLM)
            .findFirst()
            .ifPresent(otherFamily -> {
                when(asset.type().family()).thenReturn(otherFamily);
                assertFalse(resolver.accept(asset));
            });
    }

    @Test
    void resolve_whenAllSlotsHaveNoActivities_returnsEmpty() throws WombatActivityException
    {
        TimeRange range = new TimeRange(START, END, RESOLUTION);
        when(asset.identity().id()).thenReturn(ASSET_ID);
        when(metricResolver.serviceActivities(anyLong(), anyLong(), eq(ASSET_ID))).thenReturn(Map.of());

        Optional<AssetActivity> result = resolver.resolve(asset, range, filter);

        assertTrue(result.isEmpty());
    }

    @Test
    void resolve_whenActivitiesAvailable_producesActivityDataPerTimeSlot() throws WombatActivityException
    {
        TimeRange range = new TimeRange(START, END, RESOLUTION); // 2 slots: 00:00-01:00, 01:00-02:00
        Instant mid = START.plus(RESOLUTION);
        long t0 = toEpochMs(START);
        long t1 = toEpochMs(mid);
        long t2 = toEpochMs(END);

        when(asset.identity().id()).thenReturn(ASSET_ID);
        when(filter.filterServiceIds(asset)).thenReturn(Set.of("mistral-large", "gpt-4o"));

        // Slot 1 activities
        LLMServiceActivity s1 = new LLMServiceActivity(LLMProvider.mistralai, "mistral-large", FRANCE, 500L, 10.0);
        when(metricResolver.serviceActivities(t0, t1, ASSET_ID)).thenReturn(Map.of("mistral-large", s1));

        // Slot 2 activities
        LLMServiceActivity s2 = new LLMServiceActivity(LLMProvider.openai, "gpt-4o", FRANCE, 1500L, 30.0);
        when(metricResolver.serviceActivities(t1, t2, ASSET_ID)).thenReturn(Map.of("gpt-4o", s2));

        Optional<AssetActivity> result = resolver.resolve(asset, range, filter);

        assertTrue(result.isPresent());
        AssetActivity assetActivity = result.get();
        assertEquals(asset.identity(), assetActivity.assetIdentity());
        assertEquals(2, assetActivity.activity().size());

        // Verify Slot 1
        LLMActivityData data1 = (LLMActivityData) assetActivity.activity().get(0);
        assertEquals(START, data1.range().start());
        assertEquals(mid, data1.range().end());
        assertEquals(ActivityRegime.MODELED, data1.regime());
        assertEquals(Set.of("mistral-large", "gpt-4o"), data1.serviceIds());
        assertEquals(1, data1.services().size());
        assertEquals(500L, data1.outputTokenCount());
        assertEquals(10.0, data1.requestCount());

        // Verify Slot 2
        LLMActivityData data2 = (LLMActivityData) assetActivity.activity().get(1);
        assertEquals(mid, data2.range().start());
        assertEquals(END, data2.range().end());
        assertEquals(ActivityRegime.MODELED, data2.regime());
        assertEquals(Set.of("mistral-large", "gpt-4o"), data2.serviceIds());
        assertEquals(1, data2.services().size());
        assertEquals(1500L, data2.outputTokenCount());
        assertEquals(30.0, data2.requestCount());
    }

    @Test
    void resolve_whenSomeSlotsLackActivities_omitsEmptySlotsOnly() throws WombatActivityException
    {
        TimeRange range = new TimeRange(START, END, RESOLUTION); // 2 slots
        Instant mid = START.plus(RESOLUTION);
        long t0 = toEpochMs(START);
        long t1 = toEpochMs(mid);
        long t2 = toEpochMs(END);

        when(asset.identity().id()).thenReturn(ASSET_ID);

        LLMServiceActivity activity = new LLMServiceActivity(LLMProvider.mistralai, "mistral-large", FRANCE, 500L, 10.0);
        when(metricResolver.serviceActivities(t0, t1, ASSET_ID)).thenReturn(Map.of("mistral-large", activity));
        when(metricResolver.serviceActivities(t1, t2, ASSET_ID)).thenReturn(Map.of());

        Optional<AssetActivity> result = resolver.resolve(asset, range, null);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().activity().size());

        LLMActivityData data = (LLMActivityData) result.get().activity().getFirst();
        assertEquals(START, data.range().start());
        assertEquals(mid, data.range().end());
        assertEquals(500L, data.outputTokenCount());
    }

    @Test
    void resolve_whenFilterIsNull_defaultsToAllActivitiesAndUsesKeysAsServiceIds() throws WombatActivityException
    {
        TimeRange range = new TimeRange(START, START.plus(RESOLUTION), RESOLUTION); // 1 slot
        long t0 = toEpochMs(START);
        long t1 = toEpochMs(START.plus(RESOLUTION));

        when(asset.identity().id()).thenReturn(ASSET_ID);

        LLMServiceActivity s1 = new LLMServiceActivity(LLMProvider.mistralai, "mistral-large", FRANCE, 500L, 10.0);
        LLMServiceActivity s2 = new LLMServiceActivity(LLMProvider.openai, "gpt-4o", FRANCE, 1500L, 30.0);
        Map<String, LLMServiceActivity> reported = Map.of("svc-1", s1, "svc-2", s2);

        when(metricResolver.serviceActivities(t0, t1, ASSET_ID)).thenReturn(reported);

        Optional<AssetActivity> result = resolver.resolve(asset, range, null);

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
        TimeRange range = new TimeRange(START, START.plus(RESOLUTION), RESOLUTION);
        long t0 = toEpochMs(START);
        long t1 = toEpochMs(START.plus(RESOLUTION));

        when(asset.identity().id()).thenReturn(ASSET_ID);
        when(filter.filterServiceIds(asset)).thenReturn(Set.of("matched-by-key", "gpt-4o"));

        // Match 1: key is in filter ("matched-by-key")
        LLMServiceActivity s1 = new LLMServiceActivity(LLMProvider.mistralai, "other-model", FRANCE, 100L, 2.0);

        // Match 2: model is in filter ("gpt-4o")
        LLMServiceActivity s2 = new LLMServiceActivity(LLMProvider.openai, "gpt-4o", FRANCE, 200L, 4.0);

        // No match: neither key nor model is in filter
        LLMServiceActivity s3 = new LLMServiceActivity(LLMProvider.mistralai, "unrelated-model", FRANCE, 300L, 6.0);

        Map<String, LLMServiceActivity> reported = Map.of(
            "matched-by-key", s1,
            "arbitrary-key", s2,
            "unrelated-key", s3
        );

        when(metricResolver.serviceActivities(t0, t1, ASSET_ID)).thenReturn(reported);

        Optional<AssetActivity> result = resolver.resolve(asset, range, filter);

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
        TimeRange range = new TimeRange(START, START.plus(RESOLUTION), RESOLUTION);
        long t0 = toEpochMs(START);
        long t1 = toEpochMs(START.plus(RESOLUTION));

        when(asset.identity().id()).thenReturn(ASSET_ID);
        when(filter.filterServiceIds(asset)).thenReturn(Set.of("non-existent-filter"));

        LLMServiceActivity s1 = new LLMServiceActivity(LLMProvider.mistralai, "mistral-large", FRANCE, 500L, 10.0);
        when(metricResolver.serviceActivities(t0, t1, ASSET_ID)).thenReturn(Map.of("svc-1", s1));

        Optional<AssetActivity> result = resolver.resolve(asset, range, filter);

        assertTrue(result.isEmpty());
    }
}
