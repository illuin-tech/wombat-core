package tech.illuin.wombat.core.evaluation.impact.commons;

import java.util.List;
import java.util.Objects;

public record Footprint(
    FootprintImpact gwp,
    FootprintImpact pe,
    FootprintImpact adp
) {
    public static Footprint sum(List<Footprint> footprints)
    {
        if (footprints.isEmpty())
            return new Footprint(zero("kgCO2eq"), zero("MJ"), zero("kgSbeq"));

        return new Footprint(
            FootprintImpact.sum(footprints.stream().map(Footprint::gwp).toList()),
            FootprintImpact.sum(footprints.stream().map(Footprint::pe).toList()),
            FootprintImpact.sum(footprints.stream().map(Footprint::adp).toList())
        );
    }

    private static FootprintImpact zero(String unit)
    {
        return new FootprintImpact(unit, "", new FootprintImpact.FootprintImpactItem(0f, List.of()), new FootprintImpact.FootprintImpactItem(0f, List.of()));
    }

    public record FootprintImpact(
        String unit,
        String description,
        FootprintImpactItem embedded,
        FootprintImpactItem use
    ) {
        public float totalValue()
        {
            return this.embedded.value() + this.use.value();
        }

        public static FootprintImpact sum(List<FootprintImpact> impacts)
        {
            List<FootprintImpact> present = impacts.stream().filter(Objects::nonNull).toList();
            if (present.isEmpty())
                return null;
            float embedded = 0;
            float use = 0;
            for (FootprintImpact impact : present)
            {
                embedded += impact.embedded().value();
                use += impact.use().value();
            }
            FootprintImpact first = present.getFirst();
            return new FootprintImpact(
                first.unit(),
                first.description(),
                new FootprintImpact.FootprintImpactItem(embedded, List.of()),
                new FootprintImpact.FootprintImpactItem(use, List.of())
            );
        }

        public record FootprintImpactItem(
            float value,
            List<String> warnings
        ) {}
    }
}
