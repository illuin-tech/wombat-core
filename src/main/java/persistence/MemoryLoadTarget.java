package persistence;

import io.fabric8.kubernetes.api.model.metrics.v1beta1.ContainerMetrics;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemoryLoadTarget implements LoadTarget {

    private static final Logger logger = LoggerFactory.getLogger(MemoryLoadTarget.class);

    private final Map<Instant, Map<String, List<ContainerMetrics>>> registry;

    public MemoryLoadTarget() {
        this.registry = new HashMap<>();
    }

    @Override
    public void outputToTarget(Instant instant, PodMetrics podMetrics, String namespace) {
        if (!this.registry.containsKey(instant))
            this.registry.put(instant, new HashMap<>());

        Map<String, List<ContainerMetrics>> currentMetrics = this.registry.get(instant);

        podMetrics.getContainers()
            .forEach(container -> {
                logger.trace("Saving locally {}", container.toString());
                String podName = podMetrics.getMetadata().getName();
                if (currentMetrics.containsKey(podName))
                    currentMetrics.get(podName).add(container);
                else {
                    List<ContainerMetrics> metrics = new ArrayList<>();
                    metrics.add(container);
                    currentMetrics.put(podName, metrics);
                }
            });
    }

    @Override
    public double computeCpuUsage() throws NoCPUUsageException {

        // TODO: check units
        return this.registry.values().stream()
            .mapToDouble(metricMap -> metricMap.values().stream()
                .mapToDouble(metrics -> metrics.stream()
                    .mapToDouble(metric -> Double.parseDouble(metric.getUsage().get("cpu").getAmount()))
                    .sum()
                )
                .sum()
            )
            .average()
            .orElseThrow(() -> new NoCPUUsageException("Could not compute CPU Usage"));
    }
}
