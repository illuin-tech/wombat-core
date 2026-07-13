package tech.illuin.wombat.handler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import tech.illuin.wombat.handler.impl.footprint_resolver.BoaviztaFootprintResolver;
import tech.illuin.wombat.handler.impl.impact_provider.BoaviztaImpactProvider;
import tech.illuin.wombat.persistence.KubernetesMetricsPersister;

@ApplicationScoped
public class ImpactConfig
{
    @Singleton
    public KubernetesImpactService provideImpactService(
        KubernetesMetricsPersister metricsPersister,
        BoaviztaImpactProvider impactProvider,
        BoaviztaFootprintResolver footprintResolver
    )
    {
        return new KubernetesImpactService(metricsPersister, impactProvider, footprintResolver);
    }

    @Singleton
    public BoaviztaFootprintResolver provideBoaviztaFootprintResolver()
    {
        return new BoaviztaFootprintResolver();
    }
}
