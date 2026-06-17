package tech.illuin.wombat.k8s;

import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics;
import jakarta.enterprise.inject.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.persistence.LoadTarget;

import java.time.Instant;
import java.util.List;

public class K8SResourceService
{
    private final K8SMultiClusterApi k8sMultiClusterApi;
    private final Instance<LoadTarget> targets;

    private static final Logger logger = LoggerFactory.getLogger(K8SResourceService.class);

    public K8SResourceService(K8SMultiClusterApi k8sMultiClusterApi, Instance<LoadTarget> targets)
    {
        this.k8sMultiClusterApi = k8sMultiClusterApi;
        this.targets = targets;
    }

    public void listPods(Instant instant, List<ClusterProperties> clusterConfigs)
    {
        clusterConfigs.forEach(clusterConfig -> {
            String clusterId = clusterConfig.id();
            logger.debug("Persisting pods usages for kubernetes config {}", clusterConfig.id());

            List<PodMetrics> podMetricsList = this.k8sMultiClusterApi.get(clusterConfig.id())
                .orElseThrow(() -> new IllegalStateException("No client registered for cluster " + clusterId))
                .top()
                .pods()
                .inNamespace(clusterConfig.namespace())
                .metrics()
                .getItems();

            this.targets.forEach(target ->
                target.outputToTarget(instant, podMetricsList, clusterId, clusterConfig.namespace())
            );
        });
    }
}
