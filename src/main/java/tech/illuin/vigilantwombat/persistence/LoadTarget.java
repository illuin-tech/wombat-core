package tech.illuin.vigilantwombat.persistence;

import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics;
import tech.illuin.vigilantwombat.persistence.model.TimeRange;

import java.time.Instant;
import java.util.Map;

public interface LoadTarget {

    void outputToTarget(Instant instant, PodMetrics podMetrics, String namespace);

    double computeCpuUsage(TimeRange timeRange) throws NoCPUUsageException;

    Map<String, Double> getContainerShares(TimeRange timeRange) throws NoCPUUsageException;
}
