package tech.illuin.wombat.core.connector.boavizta.connector.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * @param hoursLifeTime the hardware lifetime Boavizta allocates the embedded impacts over, {@code null} for its
 *                      default for the instance
 */
public record BoaviztaUsage(
    @JsonProperty("usage_location") String usageLocation,
    @JsonProperty("time_workload") List<LoadSegment> timeWorkload,
    @JsonProperty("hours_life_time") @JsonInclude(NON_NULL) Double hoursLifeTime
) {
    public BoaviztaUsage(String usageLocation, List<LoadSegment> timeWorkload)
    {
        this(usageLocation, timeWorkload, null);
    }

    public record LoadSegment(
        @JsonProperty("time_percentage") double timePercentage,
        @JsonProperty("load_percentage") double loadPercentage
    ) {}
}
