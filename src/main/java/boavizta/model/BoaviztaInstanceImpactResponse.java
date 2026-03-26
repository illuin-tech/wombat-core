package boavizta.model;

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
            int value,
            int min,
            int max,
            List<String> warnings
        ) {}
    }
}
