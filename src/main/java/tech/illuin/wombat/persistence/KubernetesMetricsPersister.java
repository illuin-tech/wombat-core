package tech.illuin.wombat.persistence;

import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.ContainerMetrics;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.metrics.MetricRecorderService;
import tech.illuin.wombat.persistence.model.ContainerLocation;
import tech.illuin.wombat.persistence.model.TimeRange;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class KubernetesMetricsPersister
{

    private static final Logger logger = LoggerFactory.getLogger(KubernetesMetricsPersister.class);

    private final ServerMetricRepository repository;
    private final MetricRecorderService recorder;

    public KubernetesMetricsPersister(ServerMetricRepository repository, MetricRecorderService recorder)
    {
        this.repository = repository;
        this.recorder = recorder;
    }

    public void outputToTarget(Collection<PodMetrics> podMetricsList, String clusterId, String namespace)
    {
        logger.debug("Recording metrics for {} pods in {}/{}", podMetricsList.size(), clusterId, namespace);
        for (PodMetrics podMetrics : podMetricsList)
        {
            String podName = podMetrics.getMetadata().getName();
            for (ContainerMetrics cm : podMetrics.getContainers())
            {
                logger.trace("Current metrics {}", cm);
                Map<String, Quantity> usage = cm.getUsage();
                this.recorder.recordContainerCpu(clusterId, namespace, podName, cm.getName(), readCpu(usage, podName, cm.getName()));
                this.recorder.recordContainerMemory(clusterId, namespace, podName, cm.getName(), readMemory(usage, podName, cm.getName()));
            }
        }
    }

    private static double readCpu(Map<String, Quantity> usage, String pod, String container)
    {
        if (usage == null || usage.get("cpu") == null)
        {
            logger.warn("No CPU metric for container {} in pod {}, recording 0", container, pod);
            return 0.0;
        }
        String amount = usage.get("cpu").getAmount();
        try
        {
            return Double.parseDouble(amount);
        }
        catch (NumberFormatException e) {
            logger.warn("CPU value {} for container {} in pod {} is not a parseable double, recording 0", amount, container, pod);
            return 0.0;
        }
    }

    private static double readMemory(Map<String, Quantity> usage, String pod, String container)
    {
        if (usage == null || usage.get("memory") == null)
        {
            logger.warn("No memory metric for container {} in pod {}, recording 0", container, pod);
            return 0.0;
        }
        try
        {
            return Quantity.getAmountInBytes(usage.get("memory")).doubleValue();
        }
        catch (IllegalArgumentException e) {
            logger.warn("Memory value {} for container {} in pod {} is not parseable, recording 0", usage.get("memory"), container, pod);
            return 0.0;
        }
    }

    public LoadData computeLoad(TimeRange timeRange, Collection<String> clusterIds) throws NoCPUUsageException
    {
        long start = toEpochMs(timeRange.start());
        long end = toEpochMs(timeRange.end());
        List<String> clusters = clusterIds.stream().toList();

        double cpuUsage = this.repository.averageCpuPerInstant(start, end, clusters)
            .orElseThrow(() -> new NoCPUUsageException("Could not compute CPU Usage"));
        Map<String, Double> containerShares = this.repository.containerShares(start, end, clusters);
        Map<String, List<ContainerLocation>> containerLocations = this.repository.containerLocations(start, end, clusters);

        return new LoadData(cpuUsage, containerShares, containerLocations);
    }

    public double computeCpuUsage(TimeRange timeRange, Collection<String> clusterIds) throws NoCPUUsageException
    {
        return this.repository.averageCpuPerInstant(
                toEpochMs(timeRange.start()), toEpochMs(timeRange.end()), clusterIds.stream().toList())
            .orElseThrow(() -> new NoCPUUsageException("Could not compute CPU Usage"));
    }

    public Map<String, Double> getContainerShares(TimeRange timeRange, Collection<String> clusterIds)
    {
        return this.repository.containerShares(
            toEpochMs(timeRange.start()), toEpochMs(timeRange.end()), clusterIds.stream().toList());
    }

    public Map<String, List<ContainerLocation>> getContainerLocations(TimeRange timeRange, Collection<String> clusterIds)
    {
        return this.repository.containerLocations(
            toEpochMs(timeRange.start()), toEpochMs(timeRange.end()), clusterIds.stream().toList());
    }

    private static long toEpochMs(Instant instant)
    {
        if (instant.equals(Instant.MIN)) return Long.MIN_VALUE;
        if (instant.equals(Instant.MAX)) return Long.MAX_VALUE;
        return instant.toEpochMilli();
    }
}
