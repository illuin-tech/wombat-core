package tech.illuin.wombat.core.evaluation.impact.kubernetes;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

public interface KubernetesMetricResolver
{
    OptionalDouble averageCpuPerInstant(long startMs, long endMs, List<String> clusterIds);

    Map<String, Double> containerShares(long startMs, long endMs, List<String> clusterIds);

    Map<String, List<ContainerLocation>> containerLocations(long startMs, long endMs, List<String> clusterIds);
}
