package persistence;

import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics;

import java.time.Instant;

public interface LoadTarget {

    void outputToTarget(Instant instant, PodMetrics podMetrics, String namespace);

    double computeCpuUsage();
}
