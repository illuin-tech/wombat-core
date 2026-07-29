package tech.illuin.wombat.kubernetes;

import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics;
import jakarta.enterprise.inject.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.persistence.KubernetesMetricsPersister;

import java.util.List;

public class KubernetesMetricsCollector
{
    private final KubernetesMultiClusterApi multiClusterApi;
    private final Instance<KubernetesMetricsPersister> persisters;

    private static final Logger logger = LoggerFactory.getLogger(KubernetesMetricsCollector.class);

    public KubernetesMetricsCollector(KubernetesMultiClusterApi multiClusterApi, Instance<KubernetesMetricsPersister> persisters)
    {
        this.multiClusterApi = multiClusterApi;
        this.persisters = persisters;
    }

    public void listPods(KubernetesAPIAssetProperties clusterConfig)
    {
        String clusterId = clusterConfig.id();
        logger.debug("Persisting pods usages for kubernetes config {}", clusterConfig.id());

        List<PodMetrics> podMetricsList = this.multiClusterApi.get(clusterConfig.id())
            .orElseThrow(() -> new IllegalStateException("No client registered for cluster " + clusterId))
            .top()
            .pods()
            .inNamespace(clusterConfig.namespace())
            .metrics()
            .getItems();

        this.persisters.forEach(persister ->
            persister.outputToTarget(podMetricsList, clusterId, clusterConfig.namespace())
        );
    }
}
