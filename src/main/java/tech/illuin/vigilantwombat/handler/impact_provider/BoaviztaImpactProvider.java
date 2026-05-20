package tech.illuin.vigilantwombat.handler.impact_provider;

import tech.illuin.vigilantwombat.boavizta.BoaviztaClient;
import tech.illuin.vigilantwombat.boavizta.model.BoaviztaInstanceConfigResponse;
import tech.illuin.vigilantwombat.boavizta.model.BoaviztaInstanceImpactRequest;
import tech.illuin.vigilantwombat.boavizta.model.BoaviztaInstanceImpactResponse;
import tech.illuin.vigilantwombat.handler.model.BoaviztaKubernetesConfig;
import tech.illuin.vigilantwombat.handler.model.ProviderConfig;
import tech.illuin.vigilantwombat.persistence.model.TimeRange;

import java.util.List;
import java.util.Set;

public class BoaviztaImpactProvider implements ImpactProvider<BoaviztaInstanceImpactResponse> {

    private final BoaviztaClient boaviztaClient;

    public BoaviztaImpactProvider(BoaviztaClient boaviztaClient) {
        this.boaviztaClient = boaviztaClient;
    }

    @Override
    public BoaviztaInstanceImpactResponse resolveImpact(ProviderConfig config, TimeRange timeRange, double load) {
        BoaviztaKubernetesConfig k8sConfig = (BoaviztaKubernetesConfig) config;
        BoaviztaInstanceConfigResponse instanceConfig = this.getInstanceConfig(k8sConfig.provider(), k8sConfig.instanceType());
        // TODO: check units
        double loadPercentage = (load / 1000 / 1000 / 1000) / instanceConfig.vcpu().def() * 100;

        return this.getInstanceImpact(
            new BoaviztaInstanceImpactRequest(
                k8sConfig.provider(),
                k8sConfig.instanceType(),
                new BoaviztaInstanceImpactRequest.Usage(k8sConfig.location(), List.of(new BoaviztaInstanceImpactRequest.Usage.LoadSegment(100, loadPercentage)))
            ),
            k8sConfig.lifespan()
        );
    }

    private BoaviztaInstanceImpactResponse getInstanceImpact(BoaviztaInstanceImpactRequest request, int duration) {
        return this.boaviztaClient.getInstanceImpact(true, duration, Set.of("gwp", "adp", "pe"), request);
    }

    private BoaviztaInstanceConfigResponse getInstanceConfig(BoaviztaInstanceImpactRequest.Provider provider, String instanceType) {
        return this.boaviztaClient.getInstanceConfig(provider, instanceType);
    }
}
