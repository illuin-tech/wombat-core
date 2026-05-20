package tech.illuin.vigilantwombat.persistence;

import io.fabric8.kubernetes.api.model.metrics.v1beta1.ContainerMetrics;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.vigilantwombat.persistence.model.ConfigDatapoint;
import tech.illuin.vigilantwombat.persistence.model.KubernetesConfigDatapoint;
import tech.illuin.vigilantwombat.persistence.model.TimeRange;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MemoryLoadTarget implements LoadTarget {

    private static final Logger logger = LoggerFactory.getLogger(MemoryLoadTarget.class);

    private final Map<Instant, ConfigDatapoint> registry;

    public MemoryLoadTarget() {
        this.registry = new HashMap<>();
    }

    @Override
    public void outputToTarget(Instant instant, PodMetrics podMetrics, String namespace) {
        KubernetesConfigDatapoint datapoint = (KubernetesConfigDatapoint) this.registry.computeIfAbsent(
            instant,
            k -> new KubernetesConfigDatapoint(new HashMap<>())
        );

        String podName = podMetrics.getMetadata().getName();
        logger.info("Saving metrics in memory for pod {}", podName);
        podMetrics.getContainers().forEach(container -> {
            logger.trace("Saving locally {}", container.toString());
            datapoint.podMetrics().computeIfAbsent(podName, k -> new ArrayList<>()).add(container);
        });
    }

    @Override
    public double computeCpuUsage(TimeRange timeRange) throws NoCPUUsageException {
        // TODO: check units
        return filterKubernetesEntries(timeRange).values().stream()
            .mapToDouble(datapoint -> datapoint.podMetrics().values().stream()
                .mapToDouble(metrics -> metrics.stream()
                    .mapToDouble(metric -> Double.parseDouble(metric.getUsage().get("cpu").getAmount()))
                    .sum()
                )
                .sum()
            )
            .average()
            .orElseThrow(() -> new NoCPUUsageException("Could not compute CPU Usage"));
    }

    @Override
    public Map<String, Double> getContainerShares(TimeRange timeRange) throws NoCPUUsageException {
        Map<Instant, List<InstantShare>> instantShares = filterKubernetesEntries(timeRange).entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> computeInstantShare(entry.getValue().podMetrics().values().stream().flatMap(Collection::stream))
            ));

        Map<String, List<Double>> containerToUsages = new HashMap<>();
        for (List<InstantShare> shares : instantShares.values()) {
            for (InstantShare share : shares) {
                containerToUsages.computeIfAbsent(share.container(), k -> new ArrayList<>()).add(share.usage());
            }
        }

        return containerToUsages.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0d)
        ));
    }

    private Map<Instant, KubernetesConfigDatapoint> filterKubernetesEntries(TimeRange timeRange) {
        return this.registry.entrySet().stream()
            .filter(entry -> !entry.getKey().isBefore(timeRange.start()) && !entry.getKey().isAfter(timeRange.end()))
            .filter(entry -> entry.getValue() instanceof KubernetesConfigDatapoint)
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> (KubernetesConfigDatapoint) entry.getValue()));
    }

    private static List<InstantShare> computeInstantShare(Stream<ContainerMetrics> metricStream) {
        List<ContainerMetrics> metrics = metricStream.toList();
        double totalUsage = metrics.stream()
            .mapToDouble(metric -> Double.parseDouble(metric.getUsage().get("cpu").getAmount()))
            .sum();
        return metrics.stream()
            .collect(Collectors.toMap(
                ContainerMetrics::getName,
                metric -> Double.parseDouble(metric.getUsage().get("cpu").getAmount()),
                Double::sum)
            )
            .entrySet().stream()
            .map(e -> new InstantShare(e.getKey(), e.getValue() / totalUsage))
            .toList();
    }

    private record InstantShare(String container, double usage) {}
}
