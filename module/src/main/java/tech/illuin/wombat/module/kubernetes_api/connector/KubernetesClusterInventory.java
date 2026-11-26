package tech.illuin.wombat.module.kubernetes_api.connector;

import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import tech.illuin.wombat.core.asset.profile.inference.ClusterInventory;
import tech.illuin.wombat.core.asset.profile.inference.InventoryUnavailableException;
import tech.illuin.wombat.core.asset.profile.inference.ResourceAmount;
import tech.illuin.wombat.core.secret.MissingSecretException;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Reads the {@link ClusterInventory} of a cluster through its API.
 * <p>
 * The client is only built on the first read, so that assets whose profile is not inferred never connect, nor need
 * their kubeconfig where they are only evaluated.
 */
public class KubernetesClusterInventory implements ClusterInventory
{
    private final Supplier<KubernetesClient> clientFactory;
    private KubernetesClient client;

    private static final String SIDECAR_RESTART_POLICY = "Always";

    public KubernetesClusterInventory(Supplier<KubernetesClient> clientFactory)
    {
        this.clientFactory = clientFactory;
    }

    @Override
    public List<Node> nodes() throws InventoryUnavailableException
    {
        try {
            return this.client().nodes().list().getItems().stream()
                .map(node -> new Node(
                    node.getMetadata().getName(),
                    node.getSpec() != null ? node.getSpec().getProviderID() : null,
                    node.getMetadata().getLabels(),
                    node.getStatus() != null ? amount(node.getStatus().getCapacity()) : ResourceAmount.ZERO
                ))
                .toList();
        }
        catch (KubernetesClientException e) {
            throw unavailable("nodes", e);
        }
    }

    @Override
    public List<Quota> quotas(String namespace) throws InventoryUnavailableException
    {
        try {
            return this.client().resourceQuotas().inNamespace(namespace).list().getItems().stream()
                .map(quota -> new Quota(
                    namespace,
                    quota.getMetadata().getName(),
                    quota.getSpec() != null ? amounts(quota.getSpec().getHard()) : Map.of()
                ))
                .toList();
        }
        catch (KubernetesClientException e) {
            throw unavailable("resourcequotas", e);
        }
    }

    @Override
    public List<Pod> pods(Set<String> namespaces) throws InventoryUnavailableException
    {
        try {
            if (namespaces.isEmpty())
                return this.client().pods().inAnyNamespace().list().getItems().stream().map(KubernetesClusterInventory::pod).toList();

            List<Pod> pods = new ArrayList<>();
            for (String namespace : namespaces)
                this.client().pods().inNamespace(namespace).list().getItems().stream().map(KubernetesClusterInventory::pod).forEach(pods::add);
            return pods;
        }
        catch (KubernetesClientException e) {
            throw unavailable("pods", e);
        }
    }

    @Override
    public Optional<String> apiServerHost()
    {
        try {
            return Optional.ofNullable(this.client().getMasterUrl()).map(URL::getHost);
        }
        catch (InventoryUnavailableException e) {
            return Optional.empty();
        }
    }

    @Override
    public synchronized void close()
    {
        if (this.client != null)
            this.client.close();
    }

    /**
     * @throws InventoryUnavailableException when the client cannot be built, e.g. on an instance that only evaluates
     *     the asset and is not given its kubeconfig
     */
    private synchronized KubernetesClient client() throws InventoryUnavailableException
    {
        if (this.client == null)
        {
            try {
                this.client = this.clientFactory.get();
            }
            catch (MissingSecretException | IllegalArgumentException | KubernetesClientException e) {
                throw new InventoryUnavailableException("cannot connect to the cluster: " + e.getMessage(), e);
            }
        }
        return this.client;
    }

    private static Pod pod(io.fabric8.kubernetes.api.model.Pod pod)
    {
        var spec = pod.getSpec();
        return new Pod(
            pod.getMetadata().getNamespace(),
            pod.getMetadata().getName(),
            pod.getStatus() != null ? pod.getStatus().getPhase() : null,
            spec != null && spec.getContainers() != null ? spec.getContainers().stream().map(KubernetesClusterInventory::container).toList() : List.of(),
            spec != null && spec.getInitContainers() != null ? spec.getInitContainers().stream().map(KubernetesClusterInventory::container).toList() : List.of(),
            spec != null ? amount(spec.getOverhead()) : ResourceAmount.ZERO
        );
    }

    private static Container container(io.fabric8.kubernetes.api.model.Container container)
    {
        ResourceRequirements resources = container.getResources();
        return new Container(
            resources != null ? amount(resources.getRequests()) : ResourceAmount.ZERO,
            resources != null ? amount(resources.getLimits()) : ResourceAmount.ZERO,
            SIDECAR_RESTART_POLICY.equals(container.getRestartPolicy())
        );
    }

    private static ResourceAmount amount(Map<String, Quantity> quantities)
    {
        Map<String, Double> values = amounts(quantities);
        return new ResourceAmount(values.getOrDefault("cpu", 0.0), values.getOrDefault("memory", 0.0));
    }

    /**
     * @return each quantity in its base unit, cores for CPU and bytes for memory, unparseable ones being left out
     */
    private static Map<String, Double> amounts(Map<String, Quantity> quantities)
    {
        Map<String, Double> values = new HashMap<>();
        if (quantities == null)
            return values;

        quantities.forEach((key, quantity) -> {
            try {
                values.put(key, Quantity.getAmountInBytes(quantity).doubleValue());
            }
            catch (IllegalArgumentException e) {
                // Left out, as if unset
            }
        });
        return values;
    }

    private static InventoryUnavailableException unavailable(String resource, KubernetesClientException e)
    {
        if (e.getCode() == HttpURLConnection.HTTP_FORBIDDEN)
            return new InventoryUnavailableException("forbidden to list " + resource, e);
        return new InventoryUnavailableException("cannot list " + resource + ": " + e.getMessage(), e);
    }
}
