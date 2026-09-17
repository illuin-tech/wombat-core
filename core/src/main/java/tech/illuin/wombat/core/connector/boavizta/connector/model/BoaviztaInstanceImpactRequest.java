package tech.illuin.wombat.core.connector.boavizta.connector.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record BoaviztaInstanceImpactRequest(
    @JsonProperty("provider") BoaviztaServerProvider provider,
    @JsonProperty("instance_type") String instanceType,
    @JsonProperty("usage") Usage usage
)
{
    public record Usage(
        @JsonProperty("usage_location") String usageLocation,
        @JsonProperty("time_workload") List<LoadSegment> timeWorkload
    ) {
        public record LoadSegment(
            @JsonProperty("time_percentage") int timePercentage,
            @JsonProperty("load_percentage") double loadPercentage
        ) {}
    }
}
