package tech.illuin.wombat.handler;

import tech.illuin.wombat.handler.footprint_resolver.FootprintResolver;
import tech.illuin.wombat.handler.impact_provider.ImpactProvider;
import tech.illuin.wombat.persistence.LoadTarget;

public interface ServiceHandler<R> {
    LoadTarget loadTarget();

    ImpactProvider<R> impactProvider();

    FootprintResolver<R> footprintResolver();

    Class<R> providerResponseType();
}
