package tech.illuin.wombat.module.kubernetes_api.connector;

import io.fabric8.kubernetes.client.KubernetesClient;
import tech.illuin.wombat.core.secret.Secret;
import tech.illuin.wombat.module.kubernetes_api.KubernetesAPIAsset;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Hands out a client built from the kubeconfig its secret currently holds: a rotated kubeconfig gets a new client, and
 * the one it replaces is closed. An in-cluster client is built once, its token being read on every request.
 */
public final class KubernetesClientSupplier implements Supplier<KubernetesClient>, AutoCloseable
{
    private final KubernetesAPIAsset properties;
    /* null for an in-cluster client */
    private final Secret kubeconfig;
    private String builtFrom;
    private KubernetesClient client;

    public KubernetesClientSupplier(KubernetesAPIAsset properties, Secret kubeconfig)
    {
        this.properties = Objects.requireNonNull(properties, "properties cannot be null");
        this.kubeconfig = Objects.requireNonNull(kubeconfig, "kubeconfig cannot be null");
        /* Built upfront, so an unusable kubeconfig fails the source's creation rather than its first poll. */
        this.get();
    }

    /** Hands out {@code client}, an in-cluster one, which never needs rebuilding. */
    public KubernetesClientSupplier(KubernetesClient client)
    {
        this.properties = null;
        this.kubeconfig = null;
        this.client = Objects.requireNonNull(client, "client cannot be null");
    }

    /**
     * A rotation that fails is tried again on the next call.
     *
     * @throws tech.illuin.wombat.core.secret.MissingSecretException when the kubeconfig is no longer bound
     * @throws IllegalArgumentException                              when the kubeconfig cannot be read, or lacks the asset's context
     */
    @Override
    public synchronized KubernetesClient get()
    {
        if (this.kubeconfig == null)
            return this.client;
        String kubeconfig = this.kubeconfig.fetch();
        if (!kubeconfig.equals(this.builtFrom))
        {
            KubernetesClient rotated = KubernetesClientConfig.create(this.properties, kubeconfig);
            if (this.client != null)
                this.client.close();
            this.client = rotated;
            this.builtFrom = kubeconfig;
        }
        return this.client;
    }

    @Override
    public synchronized void close()
    {
        if (this.client != null)
            this.client.close();
    }
}
