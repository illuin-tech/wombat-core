package tech.illuin.wombat.k8s;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;
import tech.illuin.wombat.monitor.MonitorProperties;
import tech.illuin.wombat.persistence.LoadTarget;

import java.io.File;
import java.time.Duration;

@ApplicationScoped
public class K8SConfig
{
    @Singleton
    public K8SResourceService provideKubernetesResourceService(K8SMultiClusterApi k8sMultiClusterApi, Instance<LoadTarget> targets)
    {
        return new K8SResourceService(k8sMultiClusterApi, targets);
    }

    @Singleton
    public K8SMultiClusterApi provideApi(MonitorProperties properties)
    {
        K8SMultiClusterApi multi = new K8SMultiClusterApi();
        for (K8SProperties.ClusterProperties clusterProperties : properties.k8sConfigs().clusters())
            multi.register(clusterProperties.id(), createClient(clusterProperties));
        return multi;
    }

    private static KubernetesClient createClient(K8SProperties.ClusterProperties clusterProperties)
    {
        Config config = Config.fromKubeconfig(
            clusterProperties.context().orElse(null),
            new File(clusterProperties.configPath())
        );
        clusterProperties.readTimeout().ifPresent(d ->
            config.setRequestTimeout((int) Duration.of(d.duration(), d.unit()).toMillis())
        );
        return new KubernetesClientBuilder().withConfig(config).build();
    }
}
