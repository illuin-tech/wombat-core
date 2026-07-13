package tech.illuin.wombat.model;

import tech.illuin.wombat.monitor.AssetType;

import java.util.List;

public record Footprint(
    FootprintImpact gwp,
    FootprintImpact pe,
    FootprintImpact adp,
    String service,
    ImpactProvider impactProvider,
    AssetType type
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
