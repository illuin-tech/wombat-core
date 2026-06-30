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
        record(SqliteStepMeterRegistry.CPU_METRIC, clusterId, namespace, pod, container, cpu);
    }

    public void recordContainerMemory(
        String clusterId,
        String namespace,
        String pod,
        String container,
        double memory
    )
    {
        record(SqliteStepMeterRegistry.RAM_METRIC, clusterId, namespace, pod, container, memory);
    }

    private void record(String metric, String clusterId, String namespace, String pod, String container, double value)
    {
        DistributionSummary.builder(metric)
            .tag(SqliteStepMeterRegistry.TAG_CLUSTER, clusterId)
            .tag(SqliteStepMeterRegistry.TAG_NAMESPACE, namespace)
            .tag(SqliteStepMeterRegistry.TAG_POD, pod)
            .tag(SqliteStepMeterRegistry.TAG_CONTAINER, container)
            .register(this.registry)
            .record(value);
    }
}
