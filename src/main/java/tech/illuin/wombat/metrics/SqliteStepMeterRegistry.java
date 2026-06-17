package tech.illuin.wombat.metrics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.step.StepMeterRegistry;
import io.micrometer.core.instrument.step.StepRegistryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.persistence.DatapointRepository;
import tech.illuin.wombat.persistence.model.DatapointEntity;
import tech.illuin.wombat.persistence.model.PodMetrics;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SqliteStepMeterRegistry extends StepMeterRegistry
{

    public static final String METRIC_NAME = "wombat.k8s.container.cpu";
    public static final String TAG_CLUSTER = "cluster";
    public static final String TAG_NAMESPACE = "namespace";
    public static final String TAG_POD = "pod";
    public static final String TAG_CONTAINER = "container";

    private static final Logger logger = LoggerFactory.getLogger(SqliteStepMeterRegistry.class);
    private static final String TYPE_KUBERNETES = "KUBERNETES";

    private final DatapointRepository repository;
    private final ObjectMapper mapper;
    private final Clock clock;

    public SqliteStepMeterRegistry(
        StepRegistryConfig config,
        Clock clock,
        DatapointRepository repository,
        ObjectMapper mapper
    )
    {
        super(config, clock);
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    protected TimeUnit getBaseTimeUnit()
    {
        return TimeUnit.MILLISECONDS;
    }

    @Override
    protected void publish()
    {
        logger.info("publish() invoked at wallTime={}", this.clock.wallTime());
        Map<NamespaceKey, Map<String, Map<String, String>>> grouped = new HashMap<>();
        for (Meter meter : this.getMeters())
        {
            if (!METRIC_NAME.equals(meter.getId().getName())) continue;
            if (!(meter instanceof DistributionSummary summary)) continue;

            long count = summary.count();
            if (count == 0) continue;

            double mean = summary.totalAmount() / count;
            Meter.Id id = meter.getId();
            String cluster = id.getTag(TAG_CLUSTER);
            String namespace = id.getTag(TAG_NAMESPACE);
            String pod = id.getTag(TAG_POD);
            String container = id.getTag(TAG_CONTAINER);
            if (cluster == null || namespace == null || pod == null || container == null)
            {
                logger.warn("Skipping meter {} with missing tags", id);
                continue;
            }

            NamespaceKey key = new NamespaceKey(cluster, namespace);
            grouped.computeIfAbsent(key, k -> new HashMap<>())
                .computeIfAbsent(pod, p -> new HashMap<>())
                .put(container, Double.toString(mean));
        }

        if (grouped.isEmpty())
        {
            logger.info("No CPU samples in the last window; skipping write");
            return;
        }

        long ms = this.clock.wallTime();
        for (Map.Entry<NamespaceKey, Map<String, Map<String, String>>> entry : grouped.entrySet())
        {
            DatapointEntity row = new DatapointEntity();
            row.instantMs = ms;
            row.type = TYPE_KUBERNETES;
            row.cluster = entry.getKey().cluster();
            row.namespace = entry.getKey().namespace();
            Map<String, PodMetrics.ContainerMetrics> pods = entry.getValue().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new PodMetrics.ContainerMetrics(e.getValue())));
            row.payload = serialize(new PodMetrics(pods));
            this.repository.save(row);
            logger.debug("Persisted row {} from entry {}", row, entry);
        }
        logger.info("Persisted {} cluster/namespace row(s) at {}", grouped.size(), ms);
    }

    public void flush()
    {
        this.publish();
    }

    private String serialize(PodMetrics podMetrics)
    {
        try
        {
            return this.mapper.writeValueAsString(podMetrics);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize payload", e);
        }
    }

    private record NamespaceKey(String cluster, String namespace)
    {
        NamespaceKey
        {
            Objects.requireNonNull(cluster);
            Objects.requireNonNull(namespace);
        }
    }
}
