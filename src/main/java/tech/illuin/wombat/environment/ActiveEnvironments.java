package tech.illuin.wombat.environment;

import tech.illuin.wombat.environment.persistence.AssetEntity;
import tech.illuin.wombat.environment.persistence.AssetRepository;
import tech.illuin.wombat.environment.persistence.EnvironmentEntity;
import tech.illuin.wombat.environment.persistence.EnvironmentRepository;
import tech.illuin.wombat.monitor.AssetProperties;
import tech.illuin.wombat.monitor.Environment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ActiveEnvironments
{

    private final EnvironmentRepository repository;
    private final AssetRepository assetRepository;

    public ActiveEnvironments(EnvironmentRepository repository, AssetRepository assetRepository)
    {
        this.repository = repository;
        this.assetRepository = assetRepository;
    }

    public Map<String, Environment> environments()
    {
        Map<String, Environment> result = new LinkedHashMap<>();
        for (EnvironmentEntity entity : this.repository.findActive())
        {
            List<AssetProperties> assets = this.assetRepository.findByEnvironment(entity.id).stream()
                .map(AssetEntity::toProperties)
                .toList();
            result.put(entity.id, new Environment(entity.name, assets));
        }
        return result;
    }

    public List<AssetProperties> allAssets()
    {
        return this.environments().values().stream()
            .flatMap(environment -> environment.assets().stream())
            .toList();
    }
}
