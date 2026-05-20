package tech.illuin.vigilantwombat.model;

import java.util.List;

public record Footprint(
    FootprintImpact gwp,
    FootprintImpact pe,
    FootprintImpact adp,
    String service,
    ImpactProvider impactProvider,
    Datasource datasource
) {
    public record FootprintImpact(
        String unit,
        String description,
        FootprintImpactItem embedded,
        FootprintImpactItem use
    ) {
        public float totalValue() {
            return embedded.value() + use.value();
        }

        public record FootprintImpactItem(
            float value,
            List<String> warnings
        ) {}
    }
}
