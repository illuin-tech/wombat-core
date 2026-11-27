package tech.illuin.wombat.module.kubernetes_api.profile;

import tech.illuin.wombat.core.asset.profile.ProfileOrigin;

/**
 * Where a profile inferred from a Kubernetes cluster comes from, from the most to the least faithful to the hardware.
 */
public enum KubernetesProfileOrigin implements ProfileOrigin
{
    /** The nodes' instance type, or their capacity when their provider is unknown. */
    NODES,
    /** The namespaces' resource quotas: a slice of the cluster, not its nodes. */
    RESOURCE_QUOTAS,
    /** The resources requested by the namespaces' containers: a slice of the cluster, not its nodes. */
    CONTAINER_REQUESTS,
    /** The resource limits of the namespaces' containers, none of them requesting any. */
    CONTAINER_LIMITS,
}
