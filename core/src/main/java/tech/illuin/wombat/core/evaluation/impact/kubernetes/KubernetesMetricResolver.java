package tech.illuin.wombat.core.evaluation.impact.kubernetes;

import java.util.*;

public interface KubernetesMetricResolver
{
    Optional<Double> averageCpuPerInstant(long startMs, long endMs, List<String> assetIds);

    Map<String, Double> containerShares(long startMs, long endMs, List<String> assetIds);

    Map<String, List<ContainerLocation>> containerLocations(long startMs, long endMs, List<String> assetIds);

    Map<Long, Double> cpuTimePerBucket(long startMs, long endMs, long stepMs, List<String> assetIds, Collection<String> serviceIds);
}
