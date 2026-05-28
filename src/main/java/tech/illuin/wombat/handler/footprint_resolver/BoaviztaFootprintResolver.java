package tech.illuin.wombat.handler.footprint_resolver;

import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactResponse;
import tech.illuin.wombat.model.Datasource;
import tech.illuin.wombat.model.ImpactProvider;
import tech.illuin.wombat.model.Footprint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BoaviztaFootprintResolver implements FootprintResolver<BoaviztaInstanceImpactResponse>
{
    private static final String rebalanceWarning = "This usage has been rebalanced by the Wombat service, it does not come from the Boavizta API";

    @Override
    public Footprint resolveFootprint(BoaviztaInstanceImpactResponse impact, String service, Double share)
    {
        return new Footprint(
            resolveFootprintImpact(impact.impacts().get("gwp"), share),
            resolveFootprintImpact(impact.impacts().get("pe"), share),
            resolveFootprintImpact(impact.impacts().get("adp"), share),
            service,
            ImpactProvider.BOAVIZTA,
            Datasource.KUBERNETES
        );
    }

    private static Footprint.FootprintImpact resolveFootprintImpact(BoaviztaInstanceImpactResponse.Impact input, Double share)
    {
        List<String> embeddedWarnings = new ArrayList<>(input.embedded().warnings() == null ? Collections.emptyList() : input.embedded().warnings());
        List<String> useWarnings = new ArrayList<>(input.use().warnings() == null ? Collections.emptyList() : input.use().warnings());
        if (share != 1.0d)
        {
            embeddedWarnings.add(rebalanceWarning);
            useWarnings.add(rebalanceWarning);
        }

        return new Footprint.FootprintImpact(
            input.unit(),
            input.description(),
            new Footprint.FootprintImpact.FootprintImpactItem(
                (float) (input.embedded().value() * share),
                embeddedWarnings
            ),
            new Footprint.FootprintImpact.FootprintImpactItem(
                (float) (input.use().value() * share),
                useWarnings
            )
        );
    }
}
