package tech.illuin.wombat.module.kubernetes_api.connector;


import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;
import tech.illuin.wombat.core.secret.SecretResolver;
import tech.illuin.wombat.module.kubernetes_api.KubernetesAPIAsset;

public class KubernetesClientConfig
{
    public static KubernetesClient create(KubernetesAPIAsset properties, SecretResolver secrets)
    {
        String kubeconfig = secrets.require(properties.kubeconfigKey().value());
        Config config = Config.fromKubeconfig(properties.context()
            .map(context -> selectContext(kubeconfig, context, properties))
            .orElse(kubeconfig)
        );
        reconcileHttpsProxy(config);
        properties.readTimeout().ifPresent(timeout -> config.setRequestTimeout((int) timeout.toMillis()));
        return new KubernetesClientBuilder().withConfig(config).build();
    }

    /**
     * fabric8 only takes a context alongside a kubeconfig file (its contents-and-context variant is deprecated for
     * removal), so the context is made the kubeconfig's current one instead. An unknown context fails here, where fabric8
     * would silently fall back to the kubeconfig's current context and poll another cluster.
     */
    private static String selectContext(String kubeconfig, String context, KubernetesAPIAsset properties)
    {
        io.fabric8.kubernetes.api.model.Config parsed = Serialization.unmarshal(kubeconfig, io.fabric8.kubernetes.api.model.Config.class);
        if (parsed.getContexts() == null || parsed.getContexts().stream().noneMatch(named -> context.equals(named.getName())))
            throw new IllegalArgumentException("Context " + context + " is not in the kubeconfig of asset " + properties.identity().id());

        parsed.setCurrentContext(context);
        return Serialization.asYaml(parsed);
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
