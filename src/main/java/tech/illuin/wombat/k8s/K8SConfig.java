package tech.illuin.wombat.k8s;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;
import tech.illuin.wombat.monitor.MonitoredResources;
import tech.illuin.wombat.persistence.LoadTarget;

import java.io.File;

@ApplicationScoped
public class K8SConfig
{
    @Singleton
    public K8SResourceService provideKubernetesResourceService(K8SMultiClusterApi k8sMultiClusterApi, Instance<LoadTarget> targets)
    {
        return new K8SResourceService(k8sMultiClusterApi, targets);
    }

    @Singleton
    public K8SMultiClusterApi provideApi(MonitoredResources resources)
    {
        K8SMultiClusterApi multi = new K8SMultiClusterApi();
        resources.resources().stream()
            .filter(ClusterProperties.class::isInstance)
            .map(ClusterProperties.class::cast)
            .forEach(cluster -> multi.register(cluster.id(), createClient(cluster)));
        return multi;
    }

    private static KubernetesClient createClient(ClusterProperties clusterProperties)
    {
        Config config = Config.fromKubeconfig(
            clusterProperties.context().orElse(null),
            new File(clusterProperties.configPath())
        );
        clusterProperties.readTimeout().ifPresent(timeout ->
            config.setRequestTimeout((int) timeout.toMillis())
        );
        return new KubernetesClientBuilder().withConfig(config).build();
    }
}
