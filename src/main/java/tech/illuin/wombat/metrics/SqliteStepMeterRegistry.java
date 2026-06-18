package tech.illuin.wombat.metrics;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.step.StepMeterRegistry;
import io.micrometer.core.instrument.step.StepRegistryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.persistence.KubernetesMetricRepository;
import tech.illuin.wombat.persistence.model.KubernetesMetricEntity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SqliteStepMeterRegistry extends StepMeterRegistry
{

    public static final String CPU_METRIC = "wombat.k8s.container.cpu";
    public static final String RAM_METRIC = "wombat.k8s.container.ram";
    public static final String TAG_CLUSTER = "cluster";
    public static final String TAG_NAMESPACE = "namespace";
    public static final String TAG_POD = "pod";
    public static final String TAG_CONTAINER = "container";

    private static final Logger logger = LoggerFactory.getLogger(SqliteStepMeterRegistry.class);

    private final KubernetesMetricRepository repository;
    private final Clock clock;

    public SqliteStepMeterRegistry(
        StepRegistryConfig config,
        Clock clock,
        KubernetesMetricRepository repository
    )
    {
        super(config, clock);
        this.repository = repository;
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
        long ms = this.clock.wallTime();

        Map<ContainerKey, ContainerUsage> grouped = new LinkedHashMap<>();
        for (Meter meter : this.getMeters())
        {
            Meter.Id id = meter.getId();
            boolean isCpu = CPU_METRIC.equals(id.getName());
            boolean isRam = RAM_METRIC.equals(id.getName());
            if (!isCpu && !isRam) continue;
            if (!(meter instanceof DistributionSummary summary)) continue;

            long count = summary.count();
            if (count == 0) continue;

            String cluster = id.getTag(TAG_CLUSTER);
            String namespace = id.getTag(TAG_NAMESPACE);
            String pod = id.getTag(TAG_POD);
            String container = id.getTag(TAG_CONTAINER);
            if (cluster == null || namespace == null || pod == null || container == null)
            {
                logger.warn("Skipping meter {} with missing tags", id);
                continue;
            }

            double mean = summary.totalAmount() / count;
            ContainerUsage usage = grouped.computeIfAbsent(new ContainerKey(cluster, namespace, pod, container), k -> new ContainerUsage());
            if (isCpu) usage.cpu = mean;
            else usage.ram = mean;
        }

        if (grouped.isEmpty())
        {
            logger.info("No CPU/RAM samples in the last window; skipping write");
            return;
        }

        for (Map.Entry<ContainerKey, ContainerUsage> entry : grouped.entrySet())
        {
            ContainerKey key = entry.getKey();
            ContainerUsage usage = entry.getValue();
            KubernetesMetricEntity row = new KubernetesMetricEntity();
            row.instantMs = ms;
            row.cluster = key.cluster();
            row.namespace = key.namespace();
            row.pod = key.pod();
            row.container = key.container();
            row.cpuNanocores = usage.cpu;
            row.ramBytes = usage.ram;
            this.repository.save(row);
            logger.debug("Persisted row {}", row);
        }
        logger.info("Persisted {} container row(s) at {}", grouped.size(), ms);
    }

    public void flush()
    {
        this.publish();
    }

    private record ContainerKey(String cluster, String namespace, String pod, String container)
    {
    }

    private static final class ContainerUsage
    {
        private double cpu;
        private double ram;
    }
}
