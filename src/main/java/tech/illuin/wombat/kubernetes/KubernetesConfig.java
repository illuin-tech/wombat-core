package tech.illuin.wombat.kubernetes;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;
import tech.illuin.wombat.monitor.MonitoredEnvironments;
import tech.illuin.wombat.persistence.KubernetesMetricsPersister;

import java.io.File;

@ApplicationScoped
public class KubernetesConfig
{
    @Singleton
    public KubernetesMetricsCollector provideKubernetesMetricsCollector(KubernetesMultiClusterApi multiClusterApi, Instance<KubernetesMetricsPersister> persisters)
    {
        return new KubernetesMetricsCollector(multiClusterApi, persisters);
    }

    @Singleton
    public KubernetesMultiClusterApi provideApi(MonitoredEnvironments monitoredEnvironments)
    {
        KubernetesMultiClusterApi multi = new KubernetesMultiClusterApi();
        monitoredEnvironments.allAssets().stream()
            .filter(KubernetesAssetProperties.class::isInstance)
            .map(KubernetesAssetProperties.class::cast)
            .forEach(cluster -> multi.register(cluster.id(), createClient(cluster)));
        return multi;
    }

    private static KubernetesClient createClient(KubernetesAssetProperties properties)
    {
        Config config = Config.fromKubeconfig(
            properties.context().orElse(null),
            new File(properties.configPath())
        );
        properties.readTimeout().ifPresent(timeout ->
            config.setRequestTimeout((int) timeout.toMillis())
        );
        return new KubernetesClientBuilder().withConfig(config).build();
    }
}
