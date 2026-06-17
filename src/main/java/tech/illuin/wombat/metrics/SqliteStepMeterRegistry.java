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

import java.util.concurrent.TimeUnit;

public class SqliteStepMeterRegistry extends StepMeterRegistry
{

    public static final String METRIC_NAME = "wombat.k8s.container.cpu";
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
        int written = 0;
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

            KubernetesMetricEntity row = new KubernetesMetricEntity();
            row.instantMs = ms;
            row.cluster = cluster;
            row.namespace = namespace;
            row.pod = pod;
            row.container = container;
            row.cpu = mean;
            this.repository.save(row);
            written++;
            logger.debug("Persisted row {}", row);
        }

        if (written == 0)
        {
            logger.info("No CPU samples in the last window; skipping write");
            return;
        }
        logger.info("Persisted {} container row(s) at {}", written, ms);
    }

    public void flush()
    {
        this.publish();
    }
}
