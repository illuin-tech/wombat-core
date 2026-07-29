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
            .filter(KubernetesAPIAssetProperties.class::isInstance)
            .map(KubernetesAPIAssetProperties.class::cast)
            .forEach(cluster -> multi.register(cluster.id(), createClient(cluster)));
        return multi;
    }

    private static KubernetesClient createClient(KubernetesAPIAssetProperties properties)
    {
        Config config = Config.fromKubeconfig(
            properties.context().orElse(null),
            new File(properties.configPath())
        );
        reconcileHttpsProxy(config);
        properties.readTimeout().ifPresent(timeout ->
            config.setRequestTimeout((int) timeout.toMillis())
        );
        return new KubernetesClientBuilder().withConfig(config).build();
    }

    /**
     * fabric8 routes a kubeconfig {@code proxy-url} into the {@code httpProxy} or {@code httpsProxy} slot based on
     * the <em>proxy's</em> own scheme (see {@code KubeConfigUtils}), but selects which slot to apply based on the
     * <em>API server's</em> scheme (see {@code HttpClientUtils}). An {@code http://} proxy fronting an
     * {@code https://} API server therefore lands in {@code httpProxy} and is never applied, so the client attempts
     * a direct TLS connection and the handshake is dropped. Mirror the proxy into {@code httpsProxy} so it is honored.
     */
    private static void reconcileHttpsProxy(Config config)
    {
        String masterUrl = config.getMasterUrl();
        String httpProxy = config.getHttpProxy();
        boolean httpsProxyMissing = config.getHttpsProxy() == null || config.getHttpsProxy().isEmpty();
        if (masterUrl != null && masterUrl.startsWith("https://") && httpsProxyMissing
            && httpProxy != null && !httpProxy.isEmpty())
        {
            config.setHttpsProxy(httpProxy);
        }
    }
}
