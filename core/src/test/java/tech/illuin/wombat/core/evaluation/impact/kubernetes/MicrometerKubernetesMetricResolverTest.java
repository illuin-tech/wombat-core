package tech.illuin.wombat.core.evaluation.impact.kubernetes;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.illuin.wombat.core.source.persistence.micrometer.MicrometerTags.*;

class MicrometerKubernetesMetricResolverTest
{
    private MeterRegistry registry;
    private MicrometerKubernetesMetricResolver resolver;

    @BeforeEach
    void setUp()
    {
        this.registry = new SimpleMeterRegistry();
        this.resolver = new MicrometerKubernetesMetricResolver(this.registry);
    }

    @Test
    void averageCpuPerInstant_returnsSumAcrossMatchingAssets()
    {
        recordK8sMetric("asset-1", "service-a", "cluster-1", "ns-1", "pod-1", 100.0, 1024.0);
        recordK8sMetric("asset-1", "service-b", "cluster-1", "ns-1", "pod-2", 200.0, 1024.0);
        recordK8sMetric("asset-2", "service-c", "cluster-1", "ns-1", "pod-3", 300.0, 1024.0);

        Optional<Double> asset1Cpu = this.resolver.averageCpuPerInstant(0, 3600_000, List.of("asset-1"));
        assertTrue(asset1Cpu.isPresent());
        assertEquals(300.0, asset1Cpu.get());

        Optional<Double> allCpu = this.resolver.averageCpuPerInstant(0, 3600_000, List.of());
        assertTrue(allCpu.isPresent());
        assertEquals(600.0, allCpu.get());

        Optional<Double> unknownCpu = this.resolver.averageCpuPerInstant(0, 3600_000, List.of("unknown"));
        assertTrue(unknownCpu.isEmpty());
    }

    @Test
    void averageCpuPerInstant_emptyRegistry_returnsEmpty()
    {
        assertTrue(this.resolver.averageCpuPerInstant(0, 3600_000, List.of("asset-1")).isEmpty());
    }

    @Test
    void containerShares_computesNormalizedShares()
    {
        recordK8sMetric("asset-1", "service-a", "cluster-1", "ns-1", "pod-1", 100.0, 1024.0);
        recordK8sMetric("asset-1", "service-a", "cluster-1", "ns-1", "pod-2", 100.0, 1024.0);
        recordK8sMetric("asset-1", "service-b", "cluster-1", "ns-1", "pod-3", 200.0, 1024.0);

        Map<String, Double> shares = this.resolver.containerShares(0, 3600_000, List.of("asset-1"));
        assertEquals(2, shares.size());
        assertEquals(0.5, shares.get("service-a"));
        assertEquals(0.5, shares.get("service-b"));
    }

    @Test
    void containerShares_zeroCpu_returnsEmptyMap()
    {
        recordK8sMetric("asset-1", "service-a", "cluster-1", "ns-1", "pod-1", 0.0, 1024.0);

        Map<String, Double> shares = this.resolver.containerShares(0, 3600_000, List.of("asset-1"));
        assertTrue(shares.isEmpty());
    }

    @Test
    void containerLocations_extractsDistinctLocations()
    {
        recordK8sMetric("asset-1", "service-a", "cluster-1", "ns-1", "pod-1", 100.0, 1024.0);
        recordK8sMetric("asset-1", "service-a", "cluster-1", "ns-1", "pod-2", 100.0, 1024.0);
        recordK8sMetric("asset-1", "service-a", "cluster-2", "ns-2", "pod-3", 100.0, 1024.0);

        Map<String, List<ContainerLocation>> locations = this.resolver.containerLocations(0, 3600_000, List.of("asset-1"));
        assertEquals(1, locations.size());
        List<ContainerLocation> serviceALocations = locations.get("service-a");
        assertEquals(2, serviceALocations.size());
        assertTrue(serviceALocations.contains(new ContainerLocation("cluster-1", "ns-1")));
        assertTrue(serviceALocations.contains(new ContainerLocation("cluster-2", "ns-2")));
    }

    @Test
    void cpuTimePerBucket_calculatesCpuTimePerBucket()
    {
        recordK8sMetric("asset-1", "service-a", "cluster-1", "ns-1", "pod-1", 100.0, 1024.0);
        recordK8sMetric("asset-1", "service-b", "cluster-1", "ns-1", "pod-2", 200.0, 1024.0);

        long startMs = 1000;
        long endMs = 4000;
        long stepMs = 3600_000;
        long expectedBucket = 0;
        long spanMs = endMs - startMs; // 3000

        Map<Long, Double> cpuTimeAll = this.resolver.cpuTimePerBucket(startMs, endMs, stepMs, List.of("asset-1"), List.of());
        assertEquals(Map.of(expectedBucket, 300.0 * spanMs), cpuTimeAll);

        Map<Long, Double> cpuTimeFiltered = this.resolver.cpuTimePerBucket(startMs, endMs, stepMs, List.of("asset-1"), List.of("service-a"));
        assertEquals(Map.of(expectedBucket, 100.0 * spanMs), cpuTimeFiltered);
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
