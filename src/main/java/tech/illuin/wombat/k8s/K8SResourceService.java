package tech.illuin.wombat.k8s;

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

    public void listPods(Instant instant, List<K8SProperties.ClusterProperties> clusterConfigs)
    {
        clusterConfigs.stream().forEach(clusterConfig -> {
            String clusterId = clusterConfig.id();
            logger.info("Persisting pods usages for kubernetes config {}", clusterConfig.id());

            this.k8sMultiClusterApi.get(clusterConfig.id())
                .orElseThrow(() -> new IllegalStateException("No client registered for cluster " + clusterId))
                .top()
                .pods()
                .inNamespace(clusterConfig.namespace())
                .metrics()
                .getItems()
                .forEach(podMetrics -> this.targets
                    .forEach(target -> target.outputToTarget(instant, podMetrics, clusterId, clusterConfig.namespace()))
                );
        });
    }
}
