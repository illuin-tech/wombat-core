package tech.illuin.vigilantwombat.persistence;

import io.fabric8.kubernetes.api.model.metrics.v1beta1.ContainerMetrics;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.vigilantwombat.persistence.model.TimeRange;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        String podName = podMetrics.getMetadata().getName();
        logger.info("Saving metrics in memory for pod {}", podName);
        podMetrics.getContainers()
            .forEach(container -> {
                logger.trace("Saving locally {}", container.toString());
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
    public double computeCpuUsage(TimeRange timeRange) throws NoCPUUsageException {
        // TODO: check units
        Map<Instant, Map<String, List<ContainerMetrics>>> filteredRegistry = filterRegistryEntries(timeRange);
        return filteredRegistry.values().stream()
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

    @Override
    public Map<String, Double> getContainerShares(TimeRange timeRange) throws NoCPUUsageException
    {
        Map<Instant, Map<String, List<ContainerMetrics>>> filteredRegistry = filterRegistryEntries(timeRange);
        Map<Instant, List<InstantShare>> instantShares  = filteredRegistry.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> computeInstantShare(entry.getValue().values().stream().flatMap(Collection::stream))
            ));

        Map<String, List<Double>> containerToUsages = new HashMap<>();
        for (List<InstantShare> shares : instantShares.values()) {
            for (InstantShare share : shares) {
                String container = share.container();
                double usage = share.usage();
                containerToUsages.computeIfAbsent(container, k -> new ArrayList<>()).add(usage);
            }
        }

        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, List<Double>> entry : containerToUsages.entrySet()) {
            String container = entry.getKey();
            List<Double> usages = entry.getValue();
            double average = usages.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0d);
            result.put(container, average);
        }

        return result;
    }

    private Map<Instant, Map<String, List<ContainerMetrics>>> filterRegistryEntries(TimeRange timeRange)
    {
        return this.registry.entrySet()
            .stream()
            .filter(entry -> !entry.getKey().isBefore(timeRange.start()) && !entry.getKey().isAfter(timeRange.end()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static List<InstantShare> computeInstantShare(Stream<ContainerMetrics> metricStream)
    {
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
