package tech.illuin.wombat.metrics;

import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.step.StepRegistryConfig;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.persistence.KubernetesMetricRepository;
import tech.illuin.wombat.persistence.model.KubernetesMetricEntity;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class MetricRecorderServiceTest
{

    private static final Duration STEP = Duration.ofMinutes(5);

    @Inject
    KubernetesMetricRepository repository;

    private MockClock clock;
    private SqliteStepMeterRegistry stepRegistry;
    private MetricRecorderService recorder;

    @BeforeEach
    @Transactional
    void setUp()
    {
        this.repository.deleteAll();
        this.clock = new MockClock();
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
        this.stepRegistry = new SqliteStepMeterRegistry(config, this.clock, this.repository);
        this.recorder = new MetricRecorderService(this.stepRegistry);
    }

    @Test
    void publish_persistsMeanOfRecordedValuesForOneContainer()
    {
        this.recorder.recordContainerCpu("c1", "ns", "podA", "api", 100.0);
        this.recorder.recordContainerCpu("c1", "ns", "podA", "api", 200.0);
        this.recorder.recordContainerCpu("c1", "ns", "podA", "api", 300.0);

        long flushMs = advanceAndFlush();

        List<KubernetesMetricEntity> rows = rowsAt(flushMs);
        assertEquals(1, rows.size());
        KubernetesMetricEntity row = rows.getFirst();
        assertEquals("c1", row.cluster);
        assertEquals("ns", row.namespace);
        assertEquals("podA", row.pod);
        assertEquals("api", row.container);
        assertEquals(200.0, row.cpu, 0.001);
    }

    @Test
    void publish_persistsOneRowPerContainerWithItsOwnMean()
    {
        this.recorder.recordContainerCpu("c1", "ns", "podA", "api", 50.0);
        this.recorder.recordContainerCpu("c1", "ns", "podA", "api", 150.0);
        this.recorder.recordContainerCpu("c1", "ns", "podA", "worker", 1000.0);
        this.recorder.recordContainerCpu("c1", "ns", "podA", "worker", 3000.0);

        long flushMs = advanceAndFlush();

        List<KubernetesMetricEntity> rows = rowsAt(flushMs);
        assertEquals(2, rows.size());
        assertEquals(100.0, cpuOf(rows, "podA", "api"), 0.001);
        assertEquals(2000.0, cpuOf(rows, "podA", "worker"), 0.001);
    }

    @Test
    void publish_writesPerClusterAndNamespace()
    {
        this.recorder.recordContainerCpu("c1", "ns1", "podA", "api", 100.0);
        this.recorder.recordContainerCpu("c2", "ns2", "podB", "api", 400.0);

        long flushMs = advanceAndFlush();

        List<KubernetesMetricEntity> rows = rowsAt(flushMs);
        assertEquals(2, rows.size());
        KubernetesMetricEntity first = rows.stream().filter(r -> "c1".equals(r.cluster)).findFirst().orElseThrow();
        KubernetesMetricEntity second = rows.stream().filter(r -> "c2".equals(r.cluster)).findFirst().orElseThrow();
        assertEquals("ns1", first.namespace);
        assertEquals("ns2", second.namespace);
        assertEquals(100.0, first.cpu, 0.001);
        assertEquals(400.0, second.cpu, 0.001);
    }

    @Test
    void publish_emptyWindow_doesNotWrite()
    {
        long before = this.repository.count();
        this.clock.add(STEP.plusSeconds(1));
        this.stepRegistry.flush();
        assertEquals(before, this.repository.count(), "empty window should not produce a row");
    }

    @Test
    void publish_writesOneRowPerContainerPerWindow()
    {
        this.recorder.recordContainerCpu("c1", "ns", "podA", "api", 10.0);
        this.recorder.recordContainerCpu("c2", "ns", "podA", "api", 20.0);
        advanceAndFlush();
        this.recorder.recordContainerCpu("c1", "ns", "podA", "api", 30.0);
        advanceAndFlush();

        assertEquals(3, this.repository.count(), "first window: 2 rows (c1, c2); second window: 1 row (c1)");
    }

    @Test
    void publish_secondWindow_doesNotCarryFirstWindowValues()
    {
        this.recorder.recordContainerCpu("c1", "ns", "podA", "api", 100.0);
        this.recorder.recordContainerCpu("c1", "ns", "podA", "api", 200.0);
        long firstFlushMs = advanceAndFlush();

        this.recorder.recordContainerCpu("c1", "ns", "podA", "api", 1000.0);
        long secondFlushMs = advanceAndFlush();

        assertEquals(150.0, cpuOf(rowsAt(firstFlushMs), "podA", "api"), 0.001,
            "first window mean = (100+200)/2");
        assertEquals(1000.0, cpuOf(rowsAt(secondFlushMs), "podA", "api"), 0.001,
            "second window mean must reflect only window-2 data (no carry-over of 100/200)");
    }

    @Test
    void publish_quietWindowAfterActiveWindow_writesNothing()
    {
        this.recorder.recordContainerCpu("c1", "ns", "podA", "api", 100.0);
        advanceAndFlush();
        long countAfterFirstWindow = this.repository.count();

        advanceAndFlush();

        assertEquals(countAfterFirstWindow, this.repository.count(),
            "quiet second window must not re-persist any value from window 1");
    }

    private long advanceAndFlush()
    {
        this.clock.add(STEP.plusSeconds(1));
        this.stepRegistry.flush();
        return this.clock.wallTime();
    }

    private List<KubernetesMetricEntity> rowsAt(long instantMs)
    {
        return this.repository.findByRangeAndClusters(instantMs - 1, instantMs + 1, List.of());
    }

    private static double cpuOf(List<KubernetesMetricEntity> rows, String pod, String container)
    {
        return rows.stream()
            .filter(r -> pod.equals(r.pod) && container.equals(r.container))
            .findFirst()
            .orElseThrow()
            .cpu;
    }
}
