package boavizta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class BoaviztaConfig {
    @Singleton
    public BoaviztaService provideBoaviztaService(@RestClient BoaviztaClient boaviztaClient) {
        return new BoaviztaService(boaviztaClient);
    }
}
