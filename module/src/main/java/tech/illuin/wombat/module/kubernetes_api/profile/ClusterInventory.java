package tech.illuin.wombat.module.kubernetes_api.profile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * What a Kubernetes cluster tells about its hardware, read on demand so that a profile is only inferred from what it
 * needs. Each read fails with an {@link InventoryUnavailableException} when the credentials do not allow it.
 */
public interface ClusterInventory extends AutoCloseable
{
    /**
     * Requires {@code get, list} on {@code nodes}, a cluster-scoped resource.
     */
    List<Node> nodes() throws InventoryUnavailableException;

    /**
     * Requires {@code list} on {@code resourcequotas} in {@code namespace}.
     */
    List<Quota> quotas(String namespace) throws InventoryUnavailableException;

    /**
     * Requires {@code list} on {@code pods} in each of {@code namespaces}, or cluster-wide when it is empty.
     */
    List<Pod> pods(Set<String> namespaces) throws InventoryUnavailableException;

    /**
     * @return the host name of the API server, as the credentials address it
     */
    Optional<String> apiServerHost();

    @Override
    void close();

    /**
     * @param providerId the node's {@code spec.providerID}, null when unset
     * @param capacity the node's {@code status.capacity}
     */
    record Node(
        String name,
        String providerId,
        Map<String, String> labels,
        ResourceAmount capacity
    ) {
        public Node
        {
            labels = labels != null ? Map.copyOf(labels) : Map.of();
        }
    }

    /**
     * @param hard the quota's {@code spec.hard} limits, in cores for CPU and bytes for memory
     */
    record Quota(
        String namespace,
        String name,
        Map<String, Double> hard
    ) {
        public Quota
        {
            hard = hard != null ? Map.copyOf(hard) : Map.of();
        }
    }

    /**
     * @param phase the pod's {@code status.phase}
     * @param overhead the pod's {@code spec.overhead}, set by its RuntimeClass
     */
    record Pod(
        String namespace,
        String name,
        String phase,
        List<Container> containers,
        List<Container> initContainers,
        ResourceAmount overhead
    ) {
        public Pod
        {
            containers = containers != null ? List.copyOf(containers) : List.of();
            initContainers = initContainers != null ? List.copyOf(initContainers) : List.of();
            overhead = overhead != null ? overhead : ResourceAmount.ZERO;
        }
    }

    /**
     * @param sidecar whether this init container keeps running alongside the others ({@code restartPolicy: Always})
     */
    record Container(
        ResourceAmount requests,
        ResourceAmount limits,
        boolean sidecar
    ) {
        public Container
        {
            requests = requests != null ? requests : ResourceAmount.ZERO;
            limits = limits != null ? limits : ResourceAmount.ZERO;
        }
    }
}
