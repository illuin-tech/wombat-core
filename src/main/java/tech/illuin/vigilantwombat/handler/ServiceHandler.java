package tech.illuin.vigilantwombat.handler;

import tech.illuin.vigilantwombat.handler.footprint_resolver.FootprintResolver;
import tech.illuin.vigilantwombat.handler.impact_provider.ImpactProvider;
import tech.illuin.vigilantwombat.persistence.LoadTarget;

public interface ServiceHandler<R> {
    LoadTarget loadTarget();

    ImpactProvider<R> impactProvider();

    FootprintResolver<R> footprintResolver();

    Class<R> providerResponseType();
}
