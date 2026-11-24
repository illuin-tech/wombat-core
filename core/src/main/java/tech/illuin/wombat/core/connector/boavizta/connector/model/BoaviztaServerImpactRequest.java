package tech.illuin.wombat.core.connector.boavizta.connector.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

public record BoaviztaServerImpactRequest(
    @JsonProperty("configuration") Configuration configuration,
    @JsonProperty("usage") BoaviztaUsage usage
) {
    @JsonInclude(NON_NULL)
    public record Configuration(
        @JsonProperty("cpu") CPU cpu,
        @JsonProperty("ram") List<RAM> ram,
        @JsonProperty("disk") List<Disk> disk,
        @JsonProperty("gpu") GPU gpu
    ) {
        @JsonInclude(NON_NULL)
        public record CPU(
            @JsonProperty("units") int units,
            @JsonProperty("core_units") int coreUnits,
            @JsonProperty("name") String name
        ) {}

        public record RAM(
            @JsonProperty("units") int units,
            @JsonProperty("capacity") int capacity
        ) {}

        public record Disk(
            @JsonProperty("units") int units,
            @JsonProperty("type") String type,
            @JsonProperty("capacity") int capacity
        ) {}

        @JsonInclude(NON_NULL)
        public record GPU(
            @JsonProperty("units") int units,
            @JsonProperty("name") String name
        ) {}
    }
}
