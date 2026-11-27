package tech.illuin.wombat.module.boavizta.connector.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record BoaviztaImpactResponse(
    @JsonProperty("impacts") Map<String, Impact> impacts,
    @JsonProperty("node_count") int nodeCount,
    @JsonProperty("verbose") Verbose verbose
) {
    public record Impact(
        String unit,
        String description,
        ImpactItem embedded,
        ImpactItem use
    ) {
        public record ImpactItem(
            float value,
            float min,
            float max,
            List<String> warnings
        ) {}
    }

    /** The part of the verbose output that gives the energy behind the use-phase impacts. */
    public record Verbose(
        @JsonProperty("avg_power") Value avgPower,
        @JsonProperty("use_time_ratio") Value useTimeRatio
    ) {
        public record Value(
            double value
        ) {}
    }
}
