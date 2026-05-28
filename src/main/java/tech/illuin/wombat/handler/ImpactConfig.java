package tech.illuin.wombat.handler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import tech.illuin.wombat.handler.footprint_resolver.BoaviztaFootprintResolver;
import tech.illuin.wombat.handler.impact_provider.BoaviztaImpactProvider;
import tech.illuin.wombat.model.Datasource;
import tech.illuin.wombat.persistence.MemoryLoadTarget;

import java.util.Map;

@ApplicationScoped
public class ImpactConfig {
    @Singleton
    public ImpactService provideImpactService(BoaviztaServiceHandler boaviztaServiceHandler) {
        return new ImpactService(Map.of(
            Datasource.KUBERNETES, boaviztaServiceHandler
        ));
    }

    @Singleton
    public BoaviztaServiceHandler provideBoaviztaServiceHandler(
        MemoryLoadTarget loadTarget,
        BoaviztaImpactProvider impactProvider,
        BoaviztaFootprintResolver footprintResolver
    ) {
        return new BoaviztaServiceHandler(loadTarget, impactProvider, footprintResolver);
    }

    @Singleton
    public BoaviztaFootprintResolver provideBoaviztaFootprintResolver()
    {
        return new BoaviztaFootprintResolver();
    }
}
