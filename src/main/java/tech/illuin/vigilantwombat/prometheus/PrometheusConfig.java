package tech.illuin.vigilantwombat.prometheus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class PrometheusConfig {
    @Singleton
    public PrometheusService providePrometheusService(@RestClient PrometheusClient client) {
        return new PrometheusService(client);
    }
}
