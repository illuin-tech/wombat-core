package tech.illuin.wombat.model;

import tech.illuin.wombat.model.Footprint.FootprintImpact;
import tech.illuin.wombat.model.Footprint.FootprintImpact.FootprintImpactItem;

import java.util.List;
import java.util.Objects;

public final class Footprints
{
    private Footprints()
    {}

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
            new FootprintImpactItem(embedded, List.of()),
            new FootprintImpactItem(use, List.of())
        );
    }
}
