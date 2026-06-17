package tech.illuin.wombat.metrics;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;

public class MetricRecorderService
{

    private final MeterRegistry registry;

    public MetricRecorderService(MeterRegistry registry)
    {
        this.registry = registry;
    }

    public void recordContainerCpu(
        String clusterId,
        String namespace,
        String pod,
        String container,
        double cpu
    )
    {
        DistributionSummary.builder(SqliteStepMeterRegistry.METRIC_NAME)
            .tag(SqliteStepMeterRegistry.TAG_CLUSTER, clusterId)
            .tag(SqliteStepMeterRegistry.TAG_NAMESPACE, namespace)
            .tag(SqliteStepMeterRegistry.TAG_POD, pod)
            .tag(SqliteStepMeterRegistry.TAG_CONTAINER, container)
            .register(this.registry)
            .record(cpu);
    }
}
