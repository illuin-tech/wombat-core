package tech.illuin.wombat.module.kubernetes_api.profile;

/**
 * An amount of CPU, in cores, and of memory, in bytes.
 */
public record ResourceAmount(
    double cpuCores,
    double memoryBytes
) {
    public static final ResourceAmount ZERO = new ResourceAmount(0, 0);

    public ResourceAmount plus(ResourceAmount other)
    {
        return new ResourceAmount(this.cpuCores + other.cpuCores, this.memoryBytes + other.memoryBytes);
    }

    /**
     * @return the largest amount of each resource
     */
    public ResourceAmount max(ResourceAmount other)
    {
        return new ResourceAmount(Math.max(this.cpuCores, other.cpuCores), Math.max(this.memoryBytes, other.memoryBytes));
    }
}
