package tech.illuin.wombat.core.evaluation.impact.kubernetes;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.commons.TimeRange;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.illuin.wombat.core.source.persistence.micrometer.MicrometerTags.*;

class MicrometerKubernetesMetricResolverTest
{
    private MeterRegistry registry;
    private MicrometerKubernetesMetricResolver resolver;

    private static final TimeRange HOUR = new TimeRange(Instant.EPOCH, Instant.EPOCH.plusSeconds(3600));

    @BeforeEach
    void setUp()
    {
        this.registry = new SimpleMeterRegistry();
        this.resolver = new MicrometerKubernetesMetricResolver(this.registry);
    }

    @Test
    void averageCpuPerBucket_returnsSumAcrossMatchingAssets()
    {
        recordK8sMetric("asset-1", "service-a", "cluster-1", "ns-1", "pod-1", 100.0, 1024.0);
        recordK8sMetric("asset-1", "service-b", "cluster-1", "ns-1", "pod-2", 200.0, 1024.0);
        recordK8sMetric("asset-2", "service-c", "cluster-1", "ns-1", "pod-3", 300.0, 1024.0);

        assertEquals(Map.of(0L, 300.0), this.resolver.averageCpuPerBucket(HOUR, List.of("asset-1")));

        assertEquals(Map.of(0L, 600.0), this.resolver.averageCpuPerBucket(HOUR, List.of()));

        assertTrue(this.resolver.averageCpuPerBucket(HOUR, List.of("unknown")).isEmpty());
    }

    @Test
    void averageCpuPerBucket_emptyRegistry_returnsEmpty()
    {
        assertTrue(this.resolver.averageCpuPerBucket(HOUR, List.of("asset-1")).isEmpty());
    }

    @Test
    void averageCpuPerBucket_withOnlyNonCpuMetrics_returnsEmpty()
    {
        Tags tags = Tags.of(
            TAG_ENVIRONMENT, "env-1",
            TAG_ASSET, "asset-1",
            TAG_SERVICE, "service-a"
        );
        DistributionSummary.builder(METRIC_K8S_RAM)
            .tags(tags)
            .register(this.registry)
            .record(1024.0);

        assertTrue(this.resolver.averageCpuPerBucket(HOUR, List.of("asset-1")).isEmpty());
    }

    @Test
    void averageCpuPerBucket_withUnrelatedMeters_ignoresNonK8sMetrics()
    {
        recordK8sMetric("asset-1", "service-a", "cluster-1", "ns-1", "pod-1", 150.0, 1024.0);

        DistributionSummary.builder("unrelated.metric")
            .tags(Tags.of(TAG_ASSET, "asset-1"))
            .register(this.registry)
            .record(500.0);

        assertEquals(Map.of(0L, 150.0), this.resolver.averageCpuPerBucket(HOUR, List.of("asset-1")));
    }

    @Test
    void containerSharesPerBucket_computesNormalizedShares()
    {
        recordK8sMetric("asset-1", "service-a", "cluster-1", "ns-1", "pod-1", 100.0, 1024.0);
        recordK8sMetric("asset-1", "service-a", "cluster-1", "ns-1", "pod-2", 100.0, 1024.0);
        recordK8sMetric("asset-1", "service-b", "cluster-1", "ns-1", "pod-3", 200.0, 1024.0);

        Map<String, Double> shares = this.resolver.containerSharesPerBucket(HOUR, List.of("asset-1")).getOrDefault(0L, Map.of());
        assertEquals(2, shares.size());
        assertEquals(0.5, shares.get("service-a"));
        assertEquals(0.5, shares.get("service-b"));
    }

    @Test
    void containerSharesPerBucket_zeroCpu_returnsEmptyMap()
    {
        recordK8sMetric("asset-1", "service-a", "cluster-1", "ns-1", "pod-1", 0.0, 1024.0);

        Map<String, Double> shares = this.resolver.containerSharesPerBucket(HOUR, List.of("asset-1")).getOrDefault(0L, Map.of());
        assertTrue(shares.isEmpty());
    }

    @Test
    void containerSharesPerBucket_withMissingServiceTag_ignoresGroup()
    {
        recordK8sMetric("asset-1", "service-a", "cluster-1", "ns-1", "pod-1", 100.0, 1024.0);

        // Record a metric without TAG_SERVICE
        Tags tagsWithoutService = Tags.of(
            TAG_ENVIRONMENT, "env-1",
            TAG_ASSET, "asset-1",
            TAG_ASSET_NAME, "asset-1",
            TAG_ASSET_TYPE, "k8s",
            TAG_K8S_CLUSTER, "cluster-1",
            TAG_K8S_NAMESPACE, "ns-1",
            TAG_K8S_POD, "pod-orphan",
            TAG_K8S_CONTAINER, "orphan"
        );
        DistributionSummary.builder(METRIC_K8S_CPU)
            .tags(tagsWithoutService)
            .register(this.registry)
            .record(100.0);

        Map<String, Double> shares = this.resolver.containerSharesPerBucket(HOUR, List.of("asset-1")).getOrDefault(0L, Map.of());
        assertEquals(1, shares.size());
        assertEquals(1.0, shares.get("service-a"));
    }

    @Test
    void containerSharesPerBucket_multiPodAggregationUnderSameService()
    {
        recordK8sMetric("asset-1", "service-a", "cluster-1", "ns-1", "pod-1", 100.0, 1024.0);
        recordK8sMetric("asset-1", "service-a", "cluster-1", "ns-1", "pod-2", 300.0, 1024.0);
        recordK8sMetric("asset-1", "service-b", "cluster-1", "ns-1", "pod-3", 100.0, 1024.0);

        Map<String, Double> shares = this.resolver.containerSharesPerBucket(HOUR, List.of("asset-1")).getOrDefault(0L, Map.of());
        assertEquals(2, shares.size());
        assertEquals(0.8, shares.get("service-a"), 1e-6);
        assertEquals(0.2, shares.get("service-b"), 1e-6);
    }

    /** The registry holds a snapshot rather than a history, so every slot of the range reads the same values. */
    @Test
    void perBucketValues_areTheSnapshotRepeatedForEverySlot()
    {
        recordK8sMetric("asset-1", "service-a", "cluster-1", "ns-1", "pod-1", 100.0, 1024.0);
        recordK8sMetric("asset-1", "service-b", "cluster-1", "ns-1", "pod-2", 300.0, 1024.0);
        TimeRange range = new TimeRange(Instant.EPOCH, Instant.EPOCH.plusSeconds(3 * 3600), Duration.ofHours(1));

        assertEquals(Map.of(0L, 400.0, 3600_000L, 400.0, 7200_000L, 400.0), this.resolver.averageCpuPerBucket(range, List.of("asset-1")));
        Map<Long, Map<String, Double>> shares = this.resolver.containerSharesPerBucket(range, List.of("asset-1"));
        assertEquals(Set.of(0L, 3600_000L, 7200_000L), shares.keySet());
        assertEquals(0.75, shares.get(7200_000L).get("service-b"), 1e-6);
    }

    @Test
    void containerLocations_extractsDistinctLocations()
    {
        recordK8sMetric("asset-1", "service-a", "cluster-1", "ns-1", "pod-1", 100.0, 1024.0);
        recordK8sMetric("asset-1", "service-a", "cluster-1", "ns-1", "pod-2", 100.0, 1024.0);
        recordK8sMetric("asset-1", "service-a", "cluster-2", "ns-2", "pod-3", 100.0, 1024.0);

        Map<String, List<ContainerLocation>> locations = this.resolver.containerLocations(HOUR, List.of("asset-1"));
        assertEquals(1, locations.size());
        List<ContainerLocation> serviceALocations = locations.get("service-a");
        assertEquals(2, serviceALocations.size());
        assertTrue(serviceALocations.contains(new ContainerLocation("cluster-1", "ns-1")));
        assertTrue(serviceALocations.contains(new ContainerLocation("cluster-2", "ns-2")));
    }

    @Test
    void containerLocations_withMissingClusterOrNamespace_ignoresIncompleteTags()
    {
        recordK8sMetric("asset-1", "service-a", "cluster-1", "ns-1", "pod-1", 100.0, 1024.0);

        Tags missingCluster = Tags.of(
            TAG_ENVIRONMENT, "env-1",
            TAG_ASSET, "asset-1",
            TAG_SERVICE, "service-a",
            TAG_K8S_NAMESPACE, "ns-2"
        );
        DistributionSummary.builder(METRIC_K8S_CPU).tags(missingCluster).register(this.registry).record(100.0);

        Tags missingNamespace = Tags.of(
            TAG_ENVIRONMENT, "env-1",
            TAG_ASSET, "asset-1",
            TAG_SERVICE, "service-a",
            TAG_K8S_CLUSTER, "cluster-2"
        );
        DistributionSummary.builder(METRIC_K8S_CPU).tags(missingNamespace).register(this.registry).record(100.0);

        Tags missingService = Tags.of(
            TAG_ENVIRONMENT, "env-1",
            TAG_ASSET, "asset-1",
            TAG_K8S_CLUSTER, "cluster-3",
            TAG_K8S_NAMESPACE, "ns-3"
        );
        DistributionSummary.builder(METRIC_K8S_CPU).tags(missingService).register(this.registry).record(100.0);

        Map<String, List<ContainerLocation>> locations = this.resolver.containerLocations(HOUR, List.of("asset-1"));
        assertEquals(1, locations.size());
        List<ContainerLocation> serviceALocations = locations.get("service-a");
        assertEquals(1, serviceALocations.size());
        assertEquals(new ContainerLocation("cluster-1", "ns-1"), serviceALocations.getFirst());
    }

    @Test
    void containerLocations_multiServiceLocations()
    {
        recordK8sMetric("asset-1", "service-a", "cluster-1", "ns-1", "pod-1", 100.0, 1024.0);
        recordK8sMetric("asset-1", "service-b", "cluster-2", "ns-2", "pod-2", 100.0, 1024.0);

        Map<String, List<ContainerLocation>> locations = this.resolver.containerLocations(HOUR, List.of("asset-1"));
        assertEquals(2, locations.size());
        assertEquals(List.of(new ContainerLocation("cluster-1", "ns-1")), locations.get("service-a"));
        assertEquals(List.of(new ContainerLocation("cluster-2", "ns-2")), locations.get("service-b"));
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
