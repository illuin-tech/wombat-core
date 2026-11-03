package tech.illuin.wombat.core.activity.kubernetes;

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
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.evaluation.impact.kubernetes.ClusterInfo;
import tech.illuin.wombat.core.evaluation.impact.kubernetes.ContainerLocation;
import tech.illuin.wombat.core.evaluation.impact.kubernetes.KubernetesMetricResolver;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static tech.illuin.wombat.core.activity.commons.TimeRange.toEpochMs;

@ExtendWith(MockitoExtension.class)
class KubernetesActivityResolverTest
{
    @Mock
    private KubernetesMetricResolver metricResolver;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Asset asset;

    @Mock
    private AssetFilter filter;

    private KubernetesActivityResolver resolver;

    private final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private final Instant END = Instant.parse("2026-01-01T02:00:00Z");
    private final Duration RESOLUTION = Duration.ofHours(1);
    private final String ASSET_ID = "container-1";

    @BeforeEach
    void setUp()
    {
        resolver = new KubernetesActivityResolver(metricResolver);
    }

    @Test
    void accept_whenKubernetesContainer_returnsTrue()
    {
        when(asset.type().family()).thenReturn(ServiceFamily.KUBERNETES_CONTAINER);

        assertTrue(resolver.accept(asset));
    }

    @Test
    void accept_whenNotKubernetesContainer_returnsFalse()
    {
        when(asset.type().family()).thenReturn(null);
        assertFalse(resolver.accept(asset));

        Arrays.stream(ServiceFamily.values())
            .filter(family -> family != ServiceFamily.KUBERNETES_CONTAINER)
            .findFirst()
            .ifPresent(otherFamily -> {
                when(asset.type().family()).thenReturn(otherFamily);
                assertFalse(resolver.accept(asset));
            });
    }


    @Test
    void resolve_whenAllSlotsHaveNoMetrics_returnsEmpty() throws WombatActivityException
    {
        TimeRange range = new TimeRange(START, END, RESOLUTION);
        when(asset.identity().id()).thenReturn(ASSET_ID);
        when(metricResolver.averageCpuPerBucket(any(), anyList())).thenReturn(Map.of());

        Optional<AssetActivity> result = resolver.resolve(asset, range, filter);

        assertTrue(result.isEmpty());
        verify(metricResolver, never()).containerSharesPerBucket(any(), anyList());
    }

    @Test
    void resolve_whenMetricsAvailable_producesActivityDataPerTimeSlotFromGroupedQueries() throws WombatActivityException
    {
        TimeRange range = new TimeRange(START, END, RESOLUTION); // Produces 2 slots of 1 hour
        Instant mid = START.plus(RESOLUTION);
        long t0 = toEpochMs(START);
        long t1 = toEpochMs(mid);
        long t2 = toEpochMs(END);
        long step = RESOLUTION.toMillis();
        List<String> assetIds = List.of(ASSET_ID);

        when(asset.identity().id()).thenReturn(ASSET_ID);
        when(filter.filterServiceIds(asset)).thenReturn(Set.of("service-a"));

        when(metricResolver.averageCpuPerBucket(range, assetIds)).thenReturn(Map.of(t0, 0.45, t1, 0.90));
        when(metricResolver.containerSharesPerBucket(range, assetIds)).thenReturn(Map.of(
            t0, Map.of(ASSET_ID, 0.5),
            t1, Map.of(ASSET_ID, 0.8)
        ));
        ContainerLocation location = mock(ContainerLocation.class);
        when(location.clusterId()).thenReturn("cluster-1");
        when(location.namespace()).thenReturn("ns-1");
        when(metricResolver.containerLocations(range, assetIds)).thenReturn(Map.of(ASSET_ID, List.of(location)));

        Optional<AssetActivity> result = resolver.resolve(asset, range, filter);

        assertTrue(result.isPresent());
        AssetActivity activity = result.get();
        assertEquals(asset.identity(), activity.assetIdentity());
        assertEquals(2, activity.activity().size());

        // Validate Slot 1
        KubernetesActivityData data1 = (KubernetesActivityData) activity.activity().get(0);
        assertEquals(START, data1.range().start());
        assertEquals(mid, data1.range().end());
        assertEquals(CPUUsage.nanocores(0.45), data1.cpuUsage());
        assertEquals(Map.of(ASSET_ID, 0.5), data1.containerShares());
        assertEquals(new ClusterInfo("cluster-1", "ns-1"), data1.containerLocations().get(ASSET_ID));
        assertEquals(Set.of("service-a"), data1.serviceIds());

        // Validate Slot 2
        KubernetesActivityData data2 = (KubernetesActivityData) activity.activity().get(1);
        assertEquals(mid, data2.range().start());
        assertEquals(END, data2.range().end());
        assertEquals(CPUUsage.nanocores(0.90), data2.cpuUsage());
        assertEquals(Map.of(ASSET_ID, 0.8), data2.containerShares());
        assertEquals(new ClusterInfo("cluster-1", "ns-1"), data2.containerLocations().get(ASSET_ID));
        assertEquals(Set.of("service-a"), data2.serviceIds());
    }

    @Test
    void resolve_whenSomeSlotsLackMetrics_omitsEmptySlotsOnly() throws WombatActivityException
    {
        TimeRange range = new TimeRange(START, END, RESOLUTION); // 2 slots
        Instant mid = START.plus(RESOLUTION);
        long t0 = toEpochMs(START);
        long t2 = toEpochMs(END);
        long step = RESOLUTION.toMillis();
        List<String> assetIds = List.of(ASSET_ID);

        when(asset.identity().id()).thenReturn(ASSET_ID);
        when(metricResolver.averageCpuPerBucket(range, assetIds)).thenReturn(Map.of(t0, 0.45));
        when(metricResolver.containerSharesPerBucket(range, assetIds)).thenReturn(Map.of());
        when(metricResolver.containerLocations(range, assetIds)).thenReturn(Map.of());

        Optional<AssetActivity> result = resolver.resolve(asset, range, null);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().activity().size());

        KubernetesActivityData data = (KubernetesActivityData) result.get().activity().getFirst();
        assertEquals(START, data.range().start());
        assertEquals(mid, data.range().end());
        assertEquals(CPUUsage.nanocores(0.45), data.cpuUsage());
        assertTrue(data.containerShares().isEmpty());
    }

    @Test
    void resolve_ignoresBucketsBeyondTheLastFullSlot() throws WombatActivityException
    {
        TimeRange range = new TimeRange(START, END.plus(Duration.ofMinutes(30)), RESOLUTION); // 2 full slots, the trailing half hour is dropped
        long t0 = toEpochMs(START);
        long t2 = toEpochMs(END);
        long step = RESOLUTION.toMillis();
        List<String> assetIds = List.of(ASSET_ID);

        when(asset.identity().id()).thenReturn(ASSET_ID);
        when(metricResolver.averageCpuPerBucket(range, assetIds)).thenReturn(Map.of(t0, 0.45, t2, 0.90));
        when(metricResolver.containerSharesPerBucket(range, assetIds)).thenReturn(Map.of());
        when(metricResolver.containerLocations(range, assetIds)).thenReturn(Map.of());

        Optional<AssetActivity> result = resolver.resolve(asset, range, null);

        assertEquals(1, result.orElseThrow().activity().size());
        assertEquals(START, result.get().activity().getFirst().range().start());
    }

    @Test
    void resolve_whenFilterIsNull_defaultsToEmptyServiceIds() throws WombatActivityException
    {
        TimeRange range = new TimeRange(START, START.plus(RESOLUTION), RESOLUTION); // 1 slot
        long t0 = toEpochMs(START);
        long t1 = toEpochMs(START.plus(RESOLUTION));
        long step = RESOLUTION.toMillis();
        List<String> assetIds = List.of(ASSET_ID);

        when(asset.identity().id()).thenReturn(ASSET_ID);
        when(metricResolver.averageCpuPerBucket(range, assetIds)).thenReturn(Map.of(t0, 0.2));
        when(metricResolver.containerSharesPerBucket(range, assetIds)).thenReturn(Map.of());
        when(metricResolver.containerLocations(range, assetIds)).thenReturn(Map.of());

        Optional<AssetActivity> result = resolver.resolve(asset, range, null);

        assertTrue(result.isPresent());
        KubernetesActivityData data = (KubernetesActivityData) result.get().activity().getFirst();
        assertTrue(data.serviceIds().isEmpty());
    }

    @Test
    void resolve_containerLocations_ignoresEmptyAndKeepsFirstWhenMultiple() throws WombatActivityException
    {
        TimeRange range = new TimeRange(START, START.plus(RESOLUTION), RESOLUTION);
        long t0 = toEpochMs(START);
        long t1 = toEpochMs(START.plus(RESOLUTION));
        long step = RESOLUTION.toMillis();
        List<String> assetIds = List.of(ASSET_ID);

        when(asset.identity().id()).thenReturn(ASSET_ID);
        when(metricResolver.averageCpuPerBucket(range, assetIds)).thenReturn(Map.of(t0, 0.5));
        when(metricResolver.containerSharesPerBucket(range, assetIds)).thenReturn(Map.of());

        ContainerLocation firstLocation = mock(ContainerLocation.class);
        when(firstLocation.clusterId()).thenReturn("cluster-first");
        when(firstLocation.namespace()).thenReturn("ns-first");

        ContainerLocation secondLocation = mock(ContainerLocation.class);

        Map<String, List<ContainerLocation>> reportedLocations = Map.of(
            "empty-container", List.of(),
            "multi-container", List.of(firstLocation, secondLocation)
        );

        when(metricResolver.containerLocations(range, assetIds)).thenReturn(reportedLocations);

        Optional<AssetActivity> result = resolver.resolve(asset, range, null);

        assertTrue(result.isPresent());
        KubernetesActivityData data = (KubernetesActivityData) result.get().activity().getFirst();
        Map<String, ClusterInfo> locations = data.containerLocations();

        assertFalse(locations.containsKey("empty-container"));
        assertTrue(locations.containsKey("multi-container"));
        assertEquals(new ClusterInfo("cluster-first", "ns-first"), locations.get("multi-container"));
    }
}
