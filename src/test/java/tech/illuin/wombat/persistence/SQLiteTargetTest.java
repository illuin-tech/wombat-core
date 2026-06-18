package tech.illuin.wombat.persistence;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.ContainerMetrics;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.step.StepRegistryConfig;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.metrics.MetricRecorderService;
import tech.illuin.wombat.metrics.SqliteStepMeterRegistry;
import tech.illuin.wombat.persistence.model.ContainerLocation;
import tech.illuin.wombat.persistence.model.KubernetesMetricEntity;
import tech.illuin.wombat.persistence.model.TimeRange;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class SQLiteTargetTest
{

    @Inject
    KubernetesMetricRepository repository;

    private static final Duration STEP = Duration.ofMinutes(5);

    private SQLiteTarget target;
    private SqliteStepMeterRegistry stepRegistry;
    private MockClock clock;

    @BeforeEach
    @Transactional
    void clean() throws Exception
    {
        repository.deleteAll();
        clock = new MockClock();
        StepRegistryConfig config = new StepRegistryConfig()
        {
            @Override
            public @NonNull String prefix()
            {
                return "test";
            }

            @Override
            public @NonNull Duration step()
            {
                return STEP;
            }

            @Override
            public String get(@NonNull String key)
            {
                return null;
            }
        };
        stepRegistry = new SqliteStepMeterRegistry(config, clock, repository);
        target = new SQLiteTarget(repository, new MetricRecorderService(stepRegistry));
    }

    private void flushWindow()
    {
        clock.add(STEP.plusSeconds(1));
        stepRegistry.flush();
    }

    private void seedDatapoint(long instantMs, List<Seed> seeds)
    {
        for (Seed seed : seeds)
        {
            for (Map.Entry<String, Map<String, String>> pod : seed.pods().entrySet())
            {
                for (Map.Entry<String, String> container : pod.getValue().entrySet())
                {
                    KubernetesMetricEntity row = new KubernetesMetricEntity();
                    row.instantMs = instantMs;
                    row.cluster = seed.clusterId();
                    row.namespace = seed.namespace();
                    row.pod = pod.getKey();
                    row.container = container.getKey();
                    row.cpuNanocores = Double.parseDouble(container.getValue());
                    repository.save(row);
                }
            }
        }
    }

    @Test
    void computeCpuUsage_averagesPerInstant() throws Exception
    {
        seedDatapoint(1000L, List.of(payload("c1", "ns", Map.of(
            "podA", Map.of("ctr1", "100", "ctr2", "200")
        ))));
        seedDatapoint(2000L, List.of(payload("c1", "ns", Map.of(
            "podA", Map.of("ctr1", "300", "ctr2", "100")
        ))));

        double avg = target.computeCpuUsage(new TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(5000)), List.of());

        assertEquals(350.0, avg, 0.001);
    }

    @Test
    void computeCpuUsage_emptyDatapoints_throwsNoCPUUsage()
    {
        assertThrows(NoCPUUsageException.class, () ->
            target.computeCpuUsage(new TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1)), List.of()));
    }

    @Test
    void getContainerShares_returnsNormalizedShares() {
        seedDatapoint(1000L, List.of(payload("c1", "ns", Map.of(
            "podA", Map.of("ctr1", "100", "ctr2", "300")
        ))));

        Map<String, Double> shares = target.getContainerShares(
            new TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(5000)), List.of());

        assertEquals(0.25, shares.get("ctr1"), 0.001);
        assertEquals(0.75, shares.get("ctr2"), 0.001);
    }

    @Test
    void computeCpuUsage_clusterFilter_excludesNonMatching() throws Exception
    {
        seedDatapoint(1000L, List.of(
            payload("c1", "ns1", Map.of("pod", Map.of("ctr", "100"))),
            payload("c2", "ns2", Map.of("pod", Map.of("ctr", "500")))
        ));

        double avg = target.computeCpuUsage(
            new TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(5000)), List.of("c1"));

        assertEquals(100.0, avg, 0.001);
    }

    @Test
    void getContainerLocations_reportsAllClustersForEachContainer() {
        seedDatapoint(1000L, List.of(
            payload("c1", "ns1", Map.of("pod", Map.of("api", "10"))),
            payload("c2", "ns2", Map.of("pod", Map.of("api", "20", "worker", "30")))
        ));

        Map<String, List<ContainerLocation>> locations = target.getContainerLocations(
            new TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(5000)), List.of());

        assertEquals(2, locations.get("api").size());
        assertTrue(locations.get("api").contains(new ContainerLocation("c1", "ns1")));
        assertTrue(locations.get("api").contains(new ContainerLocation("c2", "ns2")));
        assertEquals(List.of(new ContainerLocation("c2", "ns2")), locations.get("worker"));
    }

    @Test
    void getContainerLocations_clusterFilter_excludesOthers() {
        seedDatapoint(1000L, List.of(
            payload("c1", "ns1", Map.of("pod", Map.of("api", "10"))),
            payload("c2", "ns2", Map.of("pod", Map.of("api", "20")))
        ));

        Map<String, List<ContainerLocation>> locations = target.getContainerLocations(
            new TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(5000)), List.of("c1"));

        assertEquals(List.of(new ContainerLocation("c1", "ns1")), locations.get("api"));
    }

    private Seed payload(String cluster, String namespace, Map<String, Map<String, String>> pods)
    {
        return new Seed(cluster, namespace, pods);
    }

    private record Seed(String clusterId, String namespace, Map<String, Map<String, String>> pods) {}

    @Test
    void outputToTarget_writesPodMetricsToDb() throws Exception
    {
        PodMetrics pod = buildPodMetrics("pod-a", Map.of("api", "500", "worker", "1500"));

        target.outputToTarget(Instant.ofEpochMilli(2000), List.of(pod), "c1", "ns1");
        flushWindow();

        double avg = target.computeCpuUsage(rangeAroundFlush(), List.of());
        assertEquals(2000.0, avg, 0.001);
    }

    @Test
    void outputToTarget_multiplePodsSameCluster_mergesIntoSamePayload() {
        target.outputToTarget(Instant.ofEpochMilli(3000), List.of(
            buildPodMetrics("pod-a", Map.of("api", "100")),
            buildPodMetrics("pod-b", Map.of("db", "200"))
        ), "c1", "ns1");
        flushWindow();

        Map<String, Double> shares = target.getContainerShares(rangeAroundFlush(), List.of());
        assertEquals(2, shares.size(), "both containers should appear in shares");
        assertTrue(shares.containsKey("api"));
        assertTrue(shares.containsKey("db"));
    }

    @Test
    void outputToTarget_secondCallDifferentCluster_appendsNewPayload() {
        target.outputToTarget(Instant.ofEpochMilli(4000), List.of(buildPodMetrics("pod-a", Map.of("api", "100"))), "c1", "ns1");
        target.outputToTarget(Instant.ofEpochMilli(4000), List.of(buildPodMetrics("pod-a", Map.of("api", "200"))), "c2", "ns2");
        flushWindow();

        Map<String, List<ContainerLocation>> locations = target.getContainerLocations(rangeAroundFlush(), List.of());
        assertEquals(2, locations.get("api").size());
    }

    @Test
    void outputToTarget_containerWithNullUsage_stillRecordsZero() {
        Map<String, String> usages = new LinkedHashMap<>();
        usages.put("present", "42");
        PodMetrics pod = buildPodMetricsWithExplicitNullCpu("pod-x", usages, "missing");

        target.outputToTarget(Instant.ofEpochMilli(5000), List.of(pod), "c1", "ns1");
        flushWindow();

        Map<String, Double> shares = target.getContainerShares(rangeAroundFlush(), List.of());
        assertTrue(shares.containsKey("present"));
    }

    private TimeRange rangeAroundFlush()
    {
        long flushMs = clock.wallTime();
        return new TimeRange(Instant.ofEpochMilli(flushMs - 1), Instant.ofEpochMilli(flushMs + 1));
    }

    private PodMetrics buildPodMetrics(String podName, Map<String, String> containerCpuValues)
    {
        PodMetrics pod = new PodMetrics();
        pod.setMetadata(new ObjectMetaBuilder().withName(podName).build());
        for (Map.Entry<String, String> e : containerCpuValues.entrySet())
        {
            ContainerMetrics container = new ContainerMetrics();
            container.setName(e.getKey());
            container.setUsage(Map.of("cpu", new Quantity(e.getValue())));
            pod.getContainers().add(container);
        }
        return pod;
    }

    private PodMetrics buildPodMetricsWithExplicitNullCpu(String podName, Map<String, String> validContainers, String missingContainer)
    {
        PodMetrics pod = buildPodMetrics(podName, validContainers);
        ContainerMetrics missing = new ContainerMetrics();
        missing.setName(missingContainer);
        missing.setUsage(null);
        pod.getContainers().add(missing);
        return pod;
    }
}
