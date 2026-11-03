package tech.illuin.wombat.core.activity.kubernetes;

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
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.evaluation.impact.kubernetes.ClusterInfo;
import tech.illuin.wombat.core.evaluation.impact.kubernetes.MicrometerKubernetesMetricResolver;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static tech.illuin.wombat.core.source.persistence.micrometer.MicrometerTags.*;

@ExtendWith(MockitoExtension.class)
class KubernetesActivityResolverMicrometerTest
{
    private MeterRegistry registry;
    private MicrometerKubernetesMetricResolver metricResolver;
    private KubernetesActivityResolver resolver;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Asset asset;

    @Mock
    private AssetFilter filter;

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-01-01T02:00:00Z");
    private static final Duration RESOLUTION = Duration.ofHours(1);
    private static final String ASSET_ID = "k8s-asset-1";

    @BeforeEach
    void setUp()
    {
        this.registry = new SimpleMeterRegistry();
        this.metricResolver = new MicrometerKubernetesMetricResolver(this.registry);
        this.resolver = new KubernetesActivityResolver(this.metricResolver);
    }

    @Test
    void accept_whenKubernetesContainer_returnsTrue()
    {
        when(this.asset.type().family()).thenReturn(ServiceFamily.KUBERNETES_CONTAINER);
        assertTrue(this.resolver.accept(this.asset));
    }

    @Test
    void accept_whenNotKubernetesContainer_returnsFalse()
    {
        when(this.asset.type().family()).thenReturn(null);
        assertFalse(this.resolver.accept(this.asset));

        Arrays.stream(ServiceFamily.values())
            .filter(family -> family != ServiceFamily.KUBERNETES_CONTAINER)
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
        recordK8sMetric("other-asset", "service-x", "cluster-1", "ns-1", "pod-1", 100.0, 1024.0);
        when(this.asset.identity().id()).thenReturn(ASSET_ID);
        TimeRange range = new TimeRange(START, END, RESOLUTION);

        Optional<AssetActivity> result = this.resolver.resolve(this.asset, range, this.filter);
        assertTrue(result.isEmpty());
    }

    @Test
    void resolve_withMultipleSlots_resolvesActivityDataPerSlot() throws WombatActivityException
    {
        recordK8sMetric(ASSET_ID, "service-a", "cluster-1", "ns-1", "pod-1", 100.0, 1024.0);
        recordK8sMetric(ASSET_ID, "service-b", "cluster-2", "ns-2", "pod-2", 300.0, 2048.0);

        when(this.asset.identity().id()).thenReturn(ASSET_ID);
        when(this.asset.type().regime()).thenReturn(ActivityRegime.MEASURED);
        when(this.filter.filterServiceIds(this.asset)).thenReturn(Set.of("service-a", "service-b"));

        TimeRange range = new TimeRange(START, END, RESOLUTION); // 2 slots of 1 hour
        Instant mid = START.plus(RESOLUTION);

        Optional<AssetActivity> result = this.resolver.resolve(this.asset, range, this.filter);

        assertTrue(result.isPresent());
        AssetActivity activity = result.get();
        assertEquals(this.asset.identity(), activity.assetIdentity());
        assertEquals(2, activity.activity().size());

        // Verify slot 1
        KubernetesActivityData data1 = (KubernetesActivityData) activity.activity().get(0);
        assertEquals(ActivityRegime.MEASURED, data1.regime());
        assertEquals(START, data1.range().start());
        assertEquals(mid, data1.range().end());
        assertEquals(CPUUsage.nanocores(400.0), data1.cpuUsage());
        assertEquals(0.25, data1.containerShares().get("service-a"), 1e-6);
        assertEquals(0.75, data1.containerShares().get("service-b"), 1e-6);
        assertEquals(new ClusterInfo("cluster-1", "ns-1"), data1.containerLocations().get("service-a"));
        assertEquals(new ClusterInfo("cluster-2", "ns-2"), data1.containerLocations().get("service-b"));
        assertEquals(Set.of("service-a", "service-b"), data1.serviceIds());

        // Verify slot 2
        KubernetesActivityData data2 = (KubernetesActivityData) activity.activity().get(1);
        assertEquals(ActivityRegime.MEASURED, data2.regime());
        assertEquals(mid, data2.range().start());
        assertEquals(END, data2.range().end());
        assertEquals(CPUUsage.nanocores(400.0), data2.cpuUsage());
        assertEquals(0.25, data2.containerShares().get("service-a"), 1e-6);
        assertEquals(0.75, data2.containerShares().get("service-b"), 1e-6);
        assertEquals(new ClusterInfo("cluster-1", "ns-1"), data2.containerLocations().get("service-a"));
        assertEquals(new ClusterInfo("cluster-2", "ns-2"), data2.containerLocations().get("service-b"));
        assertEquals(Set.of("service-a", "service-b"), data2.serviceIds());
    }

    @Test
    void resolve_whenFilterIsNull_defaultsToEmptyServiceIds() throws WombatActivityException
    {
        recordK8sMetric(ASSET_ID, "service-a", "cluster-1", "ns-1", "pod-1", 200.0, 1024.0);

        when(this.asset.identity().id()).thenReturn(ASSET_ID);
        TimeRange range = new TimeRange(START, START.plus(RESOLUTION), RESOLUTION);

        Optional<AssetActivity> result = this.resolver.resolve(this.asset, range, null);

        assertTrue(result.isPresent());
        KubernetesActivityData data = (KubernetesActivityData) result.get().activity().getFirst();
        assertTrue(data.serviceIds().isEmpty());
        assertEquals(CPUUsage.nanocores(200.0), data.cpuUsage());
        assertEquals(Map.of("service-a", 1.0), data.containerShares());
        assertEquals(new ClusterInfo("cluster-1", "ns-1"), data.containerLocations().get("service-a"));
    }

    @Test
    void resolve_withMultipleAssetsInRegistry_isolatesTargetAssetMetrics() throws WombatActivityException
    {
        recordK8sMetric(ASSET_ID, "service-a", "cluster-1", "ns-1", "pod-1", 150.0, 1024.0);
        recordK8sMetric("asset-2", "service-b", "cluster-2", "ns-2", "pod-2", 450.0, 2048.0);

        when(this.asset.identity().id()).thenReturn(ASSET_ID);
        TimeRange range = new TimeRange(START, START.plus(RESOLUTION), RESOLUTION);

        Optional<AssetActivity> result = this.resolver.resolve(this.asset, range, null);

        assertTrue(result.isPresent());
        KubernetesActivityData data = (KubernetesActivityData) result.get().activity().getFirst();
        assertEquals(CPUUsage.nanocores(150.0), data.cpuUsage());
        assertEquals(Map.of("service-a", 1.0), data.containerShares());
        assertEquals(Map.of("service-a", new ClusterInfo("cluster-1", "ns-1")), data.containerLocations());
    }

    private void recordK8sMetric(String assetId, String serviceId, String cluster, String namespace, String pod, double cpu, double ram)
    {
        Tags tags = Tags.of(
            TAG_ENVIRONMENT, "env-1",
            TAG_ASSET, assetId,
            TAG_ASSET_NAME, assetId,
            TAG_ASSET_TYPE, "k8s",
            TAG_SERVICE, serviceId,
            TAG_K8S_CLUSTER, cluster,
            TAG_K8S_NAMESPACE, namespace,
            TAG_K8S_POD, pod,
            TAG_K8S_CONTAINER, serviceId
        );

        DistributionSummary.builder(METRIC_K8S_CPU)
            .tags(tags)
            .register(this.registry)
            .record(cpu);

        DistributionSummary.builder(METRIC_K8S_RAM)
            .tags(tags)
            .register(this.registry)
            .record(ram);
    }
}
