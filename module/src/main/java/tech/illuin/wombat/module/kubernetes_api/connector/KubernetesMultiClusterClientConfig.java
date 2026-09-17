package tech.illuin.wombat.module.kubernetes_api.connector;


import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import tech.illuin.wombat.module.kubernetes_api.KubernetesAPIAsset;
import tech.illuin.wombat.core.context.WombatContext;

import java.io.File;

public class KubernetesMultiClusterClientConfig
{
    public static KubernetesMultiClusterClient create(WombatContext context)
    {
        KubernetesMultiClusterClient multi = new KubernetesMultiClusterClient();
        context.assets().stream()
            .filter(KubernetesAPIAsset.class::isInstance)
            .map(KubernetesAPIAsset.class::cast)
            .forEach(cluster -> multi.register(cluster.id(), createClient(cluster)));
        return multi;
    }

    private static KubernetesClient createClient(KubernetesAPIAsset properties)
    {
        Config config = Config.fromKubeconfig(
            properties.context().orElse(null),
            new File(properties.configPath())
        );
        reconcileHttpsProxy(config);
        properties.readTimeout().ifPresent(timeout -> config.setRequestTimeout((int) timeout.toMillis()));
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
        if (masterUrl != null && masterUrl.startsWith("https://") && httpsProxyMissing && httpProxy != null && !httpProxy.isEmpty())
            config.setHttpsProxy(httpProxy);
    }
}
