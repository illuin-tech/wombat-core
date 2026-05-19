package tech.illuin.vigilantwombat.handler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import tech.illuin.vigilantwombat.boavizta.BoaviztaService;
import tech.illuin.vigilantwombat.persistence.MemoryLoadTarget;

@ApplicationScoped
public class ImpactConfig {
    @Singleton
    public ImpactService provideImpactService(BoaviztaService boaviztaService, MemoryLoadTarget memoryLoadTarget) {
        return new ImpactService(boaviztaService, memoryLoadTarget);
    }
}
