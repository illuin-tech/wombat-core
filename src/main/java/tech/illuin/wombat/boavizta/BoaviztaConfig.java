package tech.illuin.wombat.boavizta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import tech.illuin.wombat.handler.impl.impact_provider.BoaviztaImpactProvider;

@ApplicationScoped
public class BoaviztaConfig
{
    @Singleton
    public BoaviztaImpactProvider provideImpactProvider(@RestClient BoaviztaClient boaviztaClient)
    {
        return new BoaviztaImpactProvider(boaviztaClient);
    }
}
