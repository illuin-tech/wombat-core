package tech.illuin.vigilantwombat.boavizta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import tech.illuin.vigilantwombat.handler.impact_provider.BoaviztaImpactProvider;

@ApplicationScoped
public class BoaviztaConfig {
    @Singleton
    public BoaviztaImpactProvider provideBoaviztaService(@RestClient BoaviztaClient boaviztaClient) {
        return new BoaviztaImpactProvider(boaviztaClient);
    }
}
