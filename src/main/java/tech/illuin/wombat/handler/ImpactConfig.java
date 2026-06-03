package tech.illuin.wombat.handler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import tech.illuin.wombat.handler.footprint_resolver.BoaviztaFootprintResolver;
import tech.illuin.wombat.handler.impact_provider.BoaviztaImpactProvider;
import tech.illuin.wombat.model.Datasource;
import tech.illuin.wombat.persistence.SQLiteTarget;
import tech.illuin.wombat.profile.ServerProfileProperties;

import java.util.Map;

@ApplicationScoped
public class ImpactConfig
{
    @Singleton
    public ImpactService provideImpactService(BoaviztaServiceHandler boaviztaServiceHandler)
    {
        return new ImpactService(Map.of(
            Datasource.KUBERNETES, boaviztaServiceHandler
        ));
    }

    @Singleton
    public BoaviztaServiceHandler provideBoaviztaServiceHandler(
        SQLiteTarget loadTarget,
        BoaviztaImpactProvider impactProvider,
        BoaviztaFootprintResolver footprintResolver
    )
    {
        return new BoaviztaServiceHandler(loadTarget, impactProvider, footprintResolver);
    }

    @Singleton
    public BoaviztaFootprintResolver provideBoaviztaFootprintResolver(ServerProfileProperties serverProfileProperties)
    {
        return new BoaviztaFootprintResolver(serverProfileProperties.lifespan());
    }
}
