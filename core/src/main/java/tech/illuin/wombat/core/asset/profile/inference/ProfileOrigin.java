package tech.illuin.wombat.core.asset.profile.inference;

/**
 * Where an evaluated profile comes from, from the most to the least faithful to the hardware.
 */
public enum ProfileOrigin
{
    /** Declared by the user. */
    DECLARED,
    /** The nodes' instance type, or their capacity when their provider is unknown. */
    NODES,
    /** The namespaces' resource quotas: a slice of the cluster, not its nodes. */
    RESOURCE_QUOTAS,
    /** The resources requested by the namespaces' containers: a slice of the cluster, not its nodes. */
    CONTAINER_REQUESTS,
    /** The resource limits of the namespaces' containers, none of them requesting any. */
    CONTAINER_LIMITS,
}
