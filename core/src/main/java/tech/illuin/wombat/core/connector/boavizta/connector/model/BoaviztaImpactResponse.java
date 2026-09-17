package tech.illuin.wombat.core.connector.boavizta.connector.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record BoaviztaImpactResponse(
    @JsonProperty("impacts") Map<String, Impact> impacts,
    @JsonProperty("node_count") int nodeCount
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
}
