package tech.illuin.vigilantwombat.handler;

import tech.illuin.vigilantwombat.handler.footprint_resolver.BoaviztaFootprintResolver;
import tech.illuin.vigilantwombat.handler.footprint_resolver.FootprintResolver;
import tech.illuin.vigilantwombat.handler.impact_provider.BoaviztaImpactProvider;
import tech.illuin.vigilantwombat.boavizta.model.BoaviztaInstanceImpactResponse;
import tech.illuin.vigilantwombat.handler.impact_provider.ImpactProvider;
import tech.illuin.vigilantwombat.persistence.LoadTarget;
import tech.illuin.vigilantwombat.persistence.MemoryLoadTarget;

public class BoaviztaServiceHandler implements ServiceHandler<BoaviztaInstanceImpactResponse> {
    private final MemoryLoadTarget loadTarget;
    private final BoaviztaImpactProvider impactProvider;
    private final BoaviztaFootprintResolver footprintResolver;

    public BoaviztaServiceHandler(
        MemoryLoadTarget loadTarget,
        BoaviztaImpactProvider impactProvider,
        BoaviztaFootprintResolver footprintResolver
    ) {
        this.loadTarget = loadTarget;
        this.impactProvider = impactProvider;
        this.footprintResolver = footprintResolver;
    }

    @Override
    public LoadTarget loadTarget() {
        return this.loadTarget;
    }

    @Override
    public ImpactProvider<BoaviztaInstanceImpactResponse> impactProvider() {
        return this.impactProvider;
    }

    @Override
    public FootprintResolver<BoaviztaInstanceImpactResponse> footprintResolver() {
        return this.footprintResolver;
    }

    @Override
    public Class<BoaviztaInstanceImpactResponse> providerResponseType() {
        return BoaviztaInstanceImpactResponse.class;
    }
}
