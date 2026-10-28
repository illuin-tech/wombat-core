package tech.illuin.wombat.core.evaluation.impact.kubernetes;

import tech.illuin.wombat.core.activity.commons.TimeRange;

import java.util.*;

public interface KubernetesMetricResolver
{
    Map<Long, Double> averageCpuPerBucket(TimeRange range, List<String> assetIds);

    Map<Long, Map<String, Double>> containerSharesPerBucket(TimeRange range, List<String> assetIds);

    Map<String, List<ContainerLocation>> containerLocations(TimeRange range, List<String> assetIds);
}
