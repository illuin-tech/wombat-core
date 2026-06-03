package tech.illuin.wombat.persistence;

import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics;
import tech.illuin.wombat.persistence.model.TimeRange;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface LoadTarget
{

    void outputToTarget(Instant instant, PodMetrics podMetrics, String clusterId, String namespace);

    double computeCpuUsage(TimeRange timeRange, List<String> clusterIds) throws NoCPUUsageException;

    Map<String, Double> getContainerShares(TimeRange timeRange, List<String> clusterIds) throws NoCPUUsageException;

    default double computeCpuUsage(TimeRange timeRange) throws NoCPUUsageException
    {
        return computeCpuUsage(timeRange, List.of());
    }

    default Map<String, Double> getContainerShares(TimeRange timeRange) throws NoCPUUsageException
    {
        return getContainerShares(timeRange, List.of());
    }
}
