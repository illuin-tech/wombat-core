package tech.illuin.wombat.boavizta.model;

import java.util.List;
import java.util.Map;

public record BoaviztaInstanceImpactResponse(Map<String, Impact> impacts) {
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
