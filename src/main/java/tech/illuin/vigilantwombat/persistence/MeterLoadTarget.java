package tech.illuin.vigilantwombat.persistence;

import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.vigilantwombat.persistence.model.TimeRange;
import tech.illuin.vigilantwombat.prometheus.PrometheusService;

import java.time.Instant;
import java.util.Map;

public class MeterLoadTarget implements LoadTarget {

    private static final Logger logger = LoggerFactory.getLogger(MeterLoadTarget.class);

    private final MeterRegistry meterRegistry;
    private final PrometheusService prometheusService;

    public MeterLoadTarget(MeterRegistry meterRegistry, PrometheusService prometheusService) {
        this.meterRegistry = meterRegistry;
        this.prometheusService = prometheusService;
    }

    @Override
    public void outputToTarget(Instant instant, PodMetrics podMetrics, String namespace) {
        String podName = podMetrics.getMetadata().getName();
        podMetrics.getContainers()
            .forEach(container ->  {
                logger.trace("CPU - {}: {} {}", podName, container.getUsage().get("cpu").getAmount(), container.getUsage().get("cpu").getFormat());
                logger.trace("RAM - {}: {} {}", podName, container.getUsage().get("memory").getAmount(), container.getUsage().get("memory").getFormat());
                this.meterRegistry.gauge(
                    "container.cpu.value",
                    Tags.of("container.name", container.getName(), "namespace", namespace, "pod.name", podName, "container.cpu.unit", container.getUsage().get("cpu").getFormat()),
                    Double.valueOf(container.getUsage().get("cpu").getAmount())
                );
                this.meterRegistry.gauge(
                    "container.memory.value",
                    Tags.of("container.name", container.getName(), "namespace", namespace, "pod.name", podName, "container.memory.unit", container.getUsage().get("memory").getFormat()),
                    Double.valueOf(container.getUsage().get("cpu").getAmount())
                );
            });
    }

    @Override
    public double computeCpuUsage(TimeRange timeRange) {
        return 0.0;
    }

    @Override
    public Map<String, Double> getContainerShares(TimeRange timeRange) throws NoCPUUsageException {
        return Map.of();
    }
}
