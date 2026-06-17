package tech.illuin.wombat.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.step.StepRegistryConfig;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.persistence.DatapointRepository;
import tech.illuin.wombat.persistence.model.DatapointEntity;
import tech.illuin.wombat.persistence.model.PodMetrics;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class MetricRecorderServiceTest
{

    private static final Duration STEP = Duration.ofMinutes(5);

    @Inject
    DatapointRepository repository;

    @Inject
    ObjectMapper mapper;

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
        this.stepRegistry = new SqliteStepMeterRegistry(config, this.clock, this.repository, this.mapper);
        this.recorder = new MetricRecorderService(this.stepRegistry);
    }

    @Test
    void publish_persistsMeanOfRecordedValuesForOneContainer() throws Exception
    {
        this.recorder.recordContainerCpu("c1", "ns", "podA", "api", 100.0);
        this.recorder.recordContainerCpu("c1", "ns", "podA", "api", 200.0);
        this.recorder.recordContainerCpu("c1", "ns", "podA", "api", 300.0);

        long flushMs = advanceAndFlush();

        List<DatapointEntity> rows = rowsAt(flushMs);
        assertEquals(1, rows.size());
        DatapointEntity row = rows.getFirst();
        assertEquals("c1", row.cluster);
        assertEquals("ns", row.namespace);
        Map<String, Map<String, String>> pods = readPods(row);
        assertEquals(200.0, Double.parseDouble(pods.get("podA").get("api")), 0.001);
    }

    @Test
    void publish_persistsMeansPerContainer() throws Exception
    {
        this.recorder.recordContainerCpu("c1", "ns", "podA", "api", 50.0);
        this.recorder.recordContainerCpu("c1", "ns", "podA", "api", 150.0);
        this.recorder.recordContainerCpu("c1", "ns", "podA", "worker", 1000.0);
        this.recorder.recordContainerCpu("c1", "ns", "podA", "worker", 3000.0);

        long flushMs = advanceAndFlush();

        List<DatapointEntity> rows = rowsAt(flushMs);
        assertEquals(1, rows.size());
        Map<String, Map<String, String>> pods = readPods(rows.getFirst());
        assertEquals(100.0, Double.parseDouble(pods.get("podA").get("api")), 0.001);
        assertEquals(2000.0, Double.parseDouble(pods.get("podA").get("worker")), 0.001);
    }

    @Test
    void publish_groupsByClusterAndNamespace() throws Exception
    {
        this.recorder.recordContainerCpu("c1", "ns1", "podA", "api", 100.0);
        this.recorder.recordContainerCpu("c2", "ns2", "podB", "api", 400.0);

        long flushMs = advanceAndFlush();

        List<DatapointEntity> rows = rowsAt(flushMs);
        assertEquals(2, rows.size());
        DatapointEntity first = rows.stream().filter(r -> "c1".equals(r.cluster)).findFirst().orElseThrow();
        DatapointEntity second = rows.stream().filter(r -> "c2".equals(r.cluster)).findFirst().orElseThrow();
        assertEquals("ns1", first.namespace);
        assertEquals("ns2", second.namespace);
        assertEquals(100.0, Double.parseDouble(readPods(first).get("podA").get("api")), 0.001);
        assertEquals(400.0, Double.parseDouble(readPods(second).get("podB").get("api")), 0.001);
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
    void publish_writesOneRowPerClusterPerWindow()
    {
        this.recorder.recordContainerCpu("c1", "ns", "podA", "api", 10.0);
        this.recorder.recordContainerCpu("c2", "ns", "podA", "api", 20.0);
        advanceAndFlush();
        this.recorder.recordContainerCpu("c1", "ns", "podA", "api", 30.0);
        advanceAndFlush();

        assertEquals(3, this.repository.count(), "first window: 2 rows (c1, c2); second window: 1 row (c1)");
    }

    @Test
    void publish_secondWindow_doesNotCarryFirstWindowValues() throws Exception
    {
        this.recorder.recordContainerCpu("c1", "ns", "podA", "api", 100.0);
        this.recorder.recordContainerCpu("c1", "ns", "podA", "api", 200.0);
        long firstFlushMs = advanceAndFlush();

        this.recorder.recordContainerCpu("c1", "ns", "podA", "api", 1000.0);
        long secondFlushMs = advanceAndFlush();

        Map<String, Map<String, String>> firstPods = readPods(rowsAt(firstFlushMs).getFirst());
        Map<String, Map<String, String>> secondPods = readPods(rowsAt(secondFlushMs).getFirst());

        assertEquals(150.0, Double.parseDouble(firstPods.get("podA").get("api")), 0.001,
            "first window mean = (100+200)/2");
        assertEquals(1000.0, Double.parseDouble(secondPods.get("podA").get("api")), 0.001,
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

    private List<DatapointEntity> rowsAt(long instantMs)
    {
        return this.repository.findByTypeRangeAndClusters("KUBERNETES", instantMs - 1, instantMs + 1, List.of());
    }

    private Map<String, Map<String, String>> readPods(DatapointEntity entity) throws Exception
    {
        assertNotNull(entity.payload);
        return this.mapper.readValue(entity.payload, PodMetrics.class).pods().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().containers()));
    }
}
