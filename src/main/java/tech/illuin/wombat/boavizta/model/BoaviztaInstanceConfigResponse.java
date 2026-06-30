package tech.illuin.wombat.boavizta.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BoaviztaInstanceConfigResponse(
    @JsonProperty("vcpu") IntConfigItem vcpu,
    @JsonProperty("memory") IntConfigItem memory,
    @JsonProperty("ssd_storage") IntConfigItem ssdStorage,
    @JsonProperty("hdd_storage") IntConfigItem hddStorage,
    @JsonProperty("gpu_units") IntConfigItem gpuUnits,
    @JsonProperty("platform") StringConfigItem platform
) {
    /**
     * Number of instances of this type required to run the given CPU load (in cores), assuming one
     * instance provides {@code vcpu} cores. Always at least one.
     */
    public int nodesRequired(double cores)
    {
        int cpus = this.vcpu != null && this.vcpu.def() != null ? this.vcpu.def() : 0;
        if (cpus <= 0) return 1;
        return Math.max(1, (int) Math.ceil(cores / cpus));
    }

    public record StringConfigItem(@JsonProperty("default") String def) {}

    public record IntConfigItem(@JsonProperty("default") Integer def) {}
}
