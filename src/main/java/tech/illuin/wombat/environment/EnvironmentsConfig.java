package tech.illuin.wombat.environment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import tech.illuin.wombat.environment.persistence.AssetRepository;
import tech.illuin.wombat.environment.persistence.EnvironmentRepository;

@ApplicationScoped
public class EnvironmentsConfig
{
    @Singleton
    public ActiveEnvironments provideActiveEnvironments(EnvironmentRepository repository, AssetRepository assetRepository)
    {
        return new ActiveEnvironments(repository, assetRepository);
    }
}
