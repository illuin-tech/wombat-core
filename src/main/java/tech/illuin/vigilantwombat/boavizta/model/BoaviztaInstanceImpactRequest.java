package tech.illuin.vigilantwombat.boavizta.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record BoaviztaInstanceImpactRequest(
    @JsonProperty("provider") Provider provider,
    @JsonProperty("instance_type") String instanceType,
    @JsonProperty("usage") Usage usage
){
    public enum Provider { aws, azure, gcp, ovhcloud, scaleway }

    public record Usage(
        @JsonProperty("usage_location") String usageLocation,
        @JsonProperty("time_workload") List<LoadSegment> timeWorkload
    ) {
        public record LoadSegment(@JsonProperty("time_percentage") int timePercentage, @JsonProperty("load_percentage") double loadPercentage) {}
    }
}
