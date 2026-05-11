package tech.illuin.vigilantwombat.boavizta;

import tech.illuin.vigilantwombat.boavizta.model.BoaviztaInstanceConfigResponse;
import tech.illuin.vigilantwombat.boavizta.model.BoaviztaInstanceImpactRequest;
import tech.illuin.vigilantwombat.boavizta.model.BoaviztaInstanceImpactResponse;

import java.util.Set;

public class BoaviztaService {

    private final BoaviztaClient boaviztaClient;

    public BoaviztaService(BoaviztaClient boaviztaClient) {
        this.boaviztaClient = boaviztaClient;
    }

    public BoaviztaInstanceImpactResponse getInstanceImpact(BoaviztaInstanceImpactRequest request, int duration) {
        return this.boaviztaClient.getInstanceImpact(true, duration, Set.of("gwp", "adp", "pe"), request);
    }

    public BoaviztaInstanceConfigResponse getInstanceConfig(BoaviztaInstanceImpactRequest.Provider provider, String instanceType)
    {
        return this.boaviztaClient.getInstanceConfig(provider, instanceType);
    }
}
