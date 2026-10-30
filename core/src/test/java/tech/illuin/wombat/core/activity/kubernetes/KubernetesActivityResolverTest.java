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
        when(metricResolver.averageCpuPerInstant(anyLong(), anyLong(), anyList())).thenReturn(Optional.empty());

        Optional<AssetActivity> result = resolver.resolve(asset, range, filter);

        assertTrue(result.isEmpty());
    }

    @Test
    void resolve_whenMetricsAvailable_producesActivityDataPerTimeSlot() throws WombatActivityException
    {
        TimeRange range = new TimeRange(START, END, RESOLUTION); // Produces 2 slots of 1 hour
        Instant mid = START.plus(RESOLUTION);
        long t0 = toEpochMs(START);
        long t1 = toEpochMs(mid);
        long t2 = toEpochMs(END);
        List<String> assetIds = List.of(ASSET_ID);

        when(asset.identity().id()).thenReturn(ASSET_ID);
        when(filter.filterServiceIds(asset)).thenReturn(Set.of("service-a"));

        // Slot 1 metrics (START -> mid)
        when(metricResolver.averageCpuPerInstant(t0, t1, assetIds)).thenReturn(Optional.of(0.45));
        when(metricResolver.containerShares(t0, t1, assetIds)).thenReturn(Map.of(ASSET_ID, 0.5));
        ContainerLocation loc1 = mock(ContainerLocation.class);
        when(loc1.clusterId()).thenReturn("cluster-1");
        when(loc1.namespace()).thenReturn("ns-1");
        when(metricResolver.containerLocations(t0, t1, assetIds)).thenReturn(Map.of(ASSET_ID, List.of(loc1)));

        // Slot 2 metrics (mid -> END)
        when(metricResolver.averageCpuPerInstant(t1, t2, assetIds)).thenReturn(Optional.of(0.90));
        when(metricResolver.containerShares(t1, t2, assetIds)).thenReturn(Map.of(ASSET_ID, 0.8));
        ContainerLocation loc2 = mock(ContainerLocation.class);
        when(loc2.clusterId()).thenReturn("cluster-2");
        when(loc2.namespace()).thenReturn("ns-2");
        when(metricResolver.containerLocations(t1, t2, assetIds)).thenReturn(Map.of(ASSET_ID, List.of(loc2)));

        Optional<AssetActivity> result = resolver.resolve(asset, range, filter);

        assertTrue(result.isPresent());
        AssetActivity activity = result.get();
        assertEquals(asset.identity(), activity.assetIdentity());
        assertEquals(2, activity.activity().size());

        // Validate Slot 1
        KubernetesActivityData data1 = (KubernetesActivityData) activity.activity().get(0);
        assertEquals(START, data1.range().start());
        assertEquals(mid, data1.range().end());
        assertEquals(0.45, data1.cpuUsage());
        assertEquals(Map.of(ASSET_ID, 0.5), data1.containerShares());
        assertEquals(new ClusterInfo("cluster-1", "ns-1"), data1.containerLocations().get(ASSET_ID));
        assertEquals(Set.of("service-a"), data1.serviceIds());

        // Validate Slot 2
        KubernetesActivityData data2 = (KubernetesActivityData) activity.activity().get(1);
        assertEquals(mid, data2.range().start());
        assertEquals(END, data2.range().end());
        assertEquals(0.90, data2.cpuUsage());
        assertEquals(Map.of(ASSET_ID, 0.8), data2.containerShares());
        assertEquals(new ClusterInfo("cluster-2", "ns-2"), data2.containerLocations().get(ASSET_ID));
        assertEquals(Set.of("service-a"), data2.serviceIds());
    }

    @Test
    void resolve_whenSomeSlotsLackMetrics_omitsEmptySlotsOnly() throws WombatActivityException
    {
        TimeRange range = new TimeRange(START, END, RESOLUTION); // 2 slots
        Instant mid = START.plus(RESOLUTION);
        long t0 = toEpochMs(START);
        long t1 = toEpochMs(mid);
        long t2 = toEpochMs(END);
        List<String> assetIds = List.of(ASSET_ID);

        when(asset.identity().id()).thenReturn(ASSET_ID);

        // Slot 1 has metrics
        when(metricResolver.averageCpuPerInstant(t0, t1, assetIds)).thenReturn(Optional.of(0.45));
        when(metricResolver.containerShares(t0, t1, assetIds)).thenReturn(Map.of());
        when(metricResolver.containerLocations(t0, t1, assetIds)).thenReturn(Map.of());

        // Slot 2 has no metrics
        when(metricResolver.averageCpuPerInstant(t1, t2, assetIds)).thenReturn(Optional.empty());

        Optional<AssetActivity> result = resolver.resolve(asset, range, null);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().activity().size());

        KubernetesActivityData data = (KubernetesActivityData) result.get().activity().getFirst();
        assertEquals(START, data.range().start());
        assertEquals(mid, data.range().end());
        assertEquals(0.45, data.cpuUsage());
    }

    @Test
    void resolve_whenFilterIsNull_defaultsToEmptyServiceIds() throws WombatActivityException
    {
        TimeRange range = new TimeRange(START, START.plus(RESOLUTION), RESOLUTION); // 1 slot
        long t0 = toEpochMs(START);
        long t1 = toEpochMs(START.plus(RESOLUTION));
        List<String> assetIds = List.of(ASSET_ID);

        when(asset.identity().id()).thenReturn(ASSET_ID);
        when(metricResolver.averageCpuPerInstant(t0, t1, assetIds)).thenReturn(Optional.of(0.2));
        when(metricResolver.containerShares(t0, t1, assetIds)).thenReturn(Map.of());
        when(metricResolver.containerLocations(t0, t1, assetIds)).thenReturn(Map.of());

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
        List<String> assetIds = List.of(ASSET_ID);

        when(asset.identity().id()).thenReturn(ASSET_ID);
        when(metricResolver.averageCpuPerInstant(t0, t1, assetIds)).thenReturn(Optional.of(0.5));
        when(metricResolver.containerShares(t0, t1, assetIds)).thenReturn(Map.of());

        ContainerLocation firstLocation = mock(ContainerLocation.class);
        when(firstLocation.clusterId()).thenReturn("cluster-first");
        when(firstLocation.namespace()).thenReturn("ns-first");

        ContainerLocation secondLocation = mock(ContainerLocation.class);

        Map<String, List<ContainerLocation>> reportedLocations = Map.of(
            "empty-container", List.of(),
            "multi-container", List.of(firstLocation, secondLocation)
        );

        when(metricResolver.containerLocations(t0, t1, assetIds)).thenReturn(reportedLocations);

        Optional<AssetActivity> result = resolver.resolve(asset, range, null);

        assertTrue(result.isPresent());
        KubernetesActivityData data = (KubernetesActivityData) result.get().activity().getFirst();
        Map<String, ClusterInfo> locations = data.containerLocations();

        assertFalse(locations.containsKey("empty-container"));
        assertTrue(locations.containsKey("multi-container"));
        assertEquals(new ClusterInfo("cluster-first", "ns-first"), locations.get("multi-container"));
    }
}
