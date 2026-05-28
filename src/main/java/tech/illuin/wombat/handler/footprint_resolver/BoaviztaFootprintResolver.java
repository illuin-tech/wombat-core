package tech.illuin.wombat.handler.footprint_resolver;

import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactResponse;
import tech.illuin.wombat.model.Datasource;
import tech.illuin.wombat.model.ImpactProvider;
import tech.illuin.wombat.model.Footprint;
import tech.illuin.wombat.persistence.model.TimeRange;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BoaviztaFootprintResolver implements FootprintResolver<BoaviztaInstanceImpactResponse>
{
    private static final String rebalanceWarning = "This usage has been rebalanced by the Wombat service, it does not come from the Boavizta API";
    private static final String timeProrationWarning = "This impact has been prorated to the requested time range by the Wombat service";

    private final int lifespanHours;

    public BoaviztaFootprintResolver(int lifespanHours)
    {
        this.lifespanHours = lifespanHours;
    }

    @Override
    public Footprint resolveFootprint(BoaviztaInstanceImpactResponse impact, String service, Double share, TimeRange timeRange)
    {
        double prorationFactor = Duration.between(timeRange.start(), timeRange.end()).toSeconds() / (double) (lifespanHours * 3600L);
        return new Footprint(
            resolveFootprintImpact(impact.impacts().get("gwp"), share, prorationFactor),
            resolveFootprintImpact(impact.impacts().get("pe"), share, prorationFactor),
            resolveFootprintImpact(impact.impacts().get("adp"), share, prorationFactor),
            service,
            ImpactProvider.BOAVIZTA,
            Datasource.KUBERNETES
        );
    }

    private static Footprint.FootprintImpact resolveFootprintImpact(BoaviztaInstanceImpactResponse.Impact input, Double share, double prorationFactor)
    {
        List<String> embeddedWarnings = new ArrayList<>(input.embedded().warnings() == null ? Collections.emptyList() : input.embedded().warnings());
        List<String> useWarnings = new ArrayList<>(input.use().warnings() == null ? Collections.emptyList() : input.use().warnings());
        if (share != 1.0d)
        {
            embeddedWarnings.add(rebalanceWarning);
            useWarnings.add(rebalanceWarning);
        }
        embeddedWarnings.add(timeProrationWarning);
        useWarnings.add(timeProrationWarning);

        return new Footprint.FootprintImpact(
            input.unit(),
            input.description(),
            new Footprint.FootprintImpact.FootprintImpactItem(
                (float) (input.embedded().value() * share * prorationFactor),
                embeddedWarnings
            ),
            new Footprint.FootprintImpact.FootprintImpactItem(
                (float) (input.use().value() * share * prorationFactor),
                useWarnings
            )
        );
    }
}
