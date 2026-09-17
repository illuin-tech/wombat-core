package tech.illuin.wombat.core.evaluation.impact.kubernetes;

public record ServerSpecification(
    Integer vcpu,
    Integer memory,
    Integer ssdStorage,
    Integer hddStorage,
    Integer gpuUnits,
    String platform
) {
    /**
     * Number of instances of this type required to run the given CPU load (in cores), assuming one
     * instance provides {@code vcpu} cores. Always at least one.
     */
    public int nodesRequired(double cores)
    {
        int cpus = this.vcpu != null ? this.vcpu : 0;
        if (cpus <= 0)
            return 1;
        return Math.max(1, (int) Math.ceil(cores / cpus));
    }
}
