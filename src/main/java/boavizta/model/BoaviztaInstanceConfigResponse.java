package boavizta.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BoaviztaInstanceConfigResponse(
    @JsonProperty("vcpu") IntConfigItem vcpu,
    @JsonProperty("memory") IntConfigItem memory,
    @JsonProperty("ssd_storage") IntConfigItem ssdStorage,
    @JsonProperty("hdd_storage") IntConfigItem hddStorage,
    @JsonProperty("gpu_units") IntConfigItem gpuUnits,
    @JsonProperty("platform") StringConfigItem platform
) {
    public record StringConfigItem(@JsonProperty("default") String def) {}

    public record IntConfigItem(@JsonProperty("default") Integer def) {}
}
