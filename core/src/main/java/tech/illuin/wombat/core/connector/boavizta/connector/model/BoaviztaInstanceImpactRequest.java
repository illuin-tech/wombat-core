package tech.illuin.wombat.core.connector.boavizta.connector.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record BoaviztaInstanceImpactRequest(
    @JsonProperty("provider") BoaviztaServerProvider provider,
    @JsonProperty("instance_type") String instanceType,
    @JsonProperty("usage") Usage usage
)
{
    /**
     * @param hoursLifeTime the hardware lifetime Boavizta allocates the embedded impacts over, {@code null} for its
     *                      default for the instance
     */
    public record Usage(
        @JsonProperty("usage_location") String usageLocation,
        @JsonProperty("time_workload") List<LoadSegment> timeWorkload,
        @JsonProperty("hours_life_time") @JsonInclude(JsonInclude.Include.NON_NULL) Double hoursLifeTime
    ) {
        public Usage(String usageLocation, List<LoadSegment> timeWorkload)
        {
            this(usageLocation, timeWorkload, null);
        }

        public record LoadSegment(
            @JsonProperty("time_percentage") int timePercentage,
            @JsonProperty("load_percentage") double loadPercentage
        ) {}
    }
}
