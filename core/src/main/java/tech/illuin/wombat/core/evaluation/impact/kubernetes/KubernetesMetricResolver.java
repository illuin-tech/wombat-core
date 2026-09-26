package tech.illuin.wombat.core.evaluation.impact.kubernetes;

import java.util.*;

public interface KubernetesMetricResolver
{
    Optional<Double> averageCpuPerInstant(long startMs, long endMs, List<String> clusterIds);

    Map<String, Double> containerShares(long startMs, long endMs, List<String> clusterIds);

    Map<String, List<ContainerLocation>> containerLocations(long startMs, long endMs, List<String> clusterIds);

    Map<Long, Double> cpuTimePerBucket(long startMs, long endMs, long stepMs, List<String> clusterIds, Collection<String> serviceIds);
}
