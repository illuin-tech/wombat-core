package tech.illuin.wombat.handler.model;

import tech.illuin.wombat.model.Footprint;
import tech.illuin.wombat.model.Footprint.FootprintImpact;
import tech.illuin.wombat.model.Footprints;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public record GlobalImpact(
    Impact gwp,
    Impact pe,
    Impact adp
)
{
    public static GlobalImpact from(List<Footprint> footprints)
    {
        return new GlobalImpact(
            combine(footprints, Footprint::gwp),
            combine(footprints, Footprint::pe),
            combine(footprints, Footprint::adp)
        );
    }

    private static Impact combine(List<Footprint> footprints, Function<Footprint, FootprintImpact> criterion)
    {
        FootprintImpact sum = Footprints.sum(
            footprints.stream().filter(Objects::nonNull).map(criterion).toList()
        );
        if (sum == null)
            return null;
        float embedded = sum.embedded().value();
        float use = sum.use().value();
        return new Impact(sum.unit(), sum.description(), embedded, use, embedded + use);
    }

    public record Impact(
        String unit,
        String description,
        float embedded,
        float use,
        float total
    ) {}
}
