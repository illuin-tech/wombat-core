package tech.illuin.wombat.environment;

import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.environment.persistence.AssetEntity;
import tech.illuin.wombat.environment.persistence.AssetRepository;
import tech.illuin.wombat.environment.persistence.EnvironmentEntity;
import tech.illuin.wombat.environment.persistence.EnvironmentRepository;
import tech.illuin.wombat.monitor.AssetProperties;
import tech.illuin.wombat.monitor.Environment;
import tech.illuin.wombat.monitor.MonitoredEnvironments;

import java.util.Map;

@ApplicationScoped
public class EnvironmentSeeder
{

    static final int STARTUP_PRIORITY_SEED = 2000;

    private static final Logger logger = LoggerFactory.getLogger(EnvironmentSeeder.class);

    private final EnvironmentRepository repository;
    private final AssetRepository assetRepository;
    private final MonitoredEnvironments monitoredEnvironments;

    public EnvironmentSeeder(EnvironmentRepository repository, AssetRepository assetRepository, MonitoredEnvironments monitoredEnvironments)
    {
        this.repository = repository;
        this.assetRepository = assetRepository;
        this.monitoredEnvironments = monitoredEnvironments;
    }

    @Transactional
    void onStart(@Observes @Priority(STARTUP_PRIORITY_SEED) StartupEvent event)
    {
        int environmentsAdded = 0;
        int assetsAdded = 0;
        for (Map.Entry<String, Environment> entry : this.monitoredEnvironments.environments().entrySet())
        {
            if (this.seedEnvironment(entry.getKey(), entry.getValue()))
                environmentsAdded++;
            for (AssetProperties asset : entry.getValue().assets())
            {
                if (this.seedAsset(entry.getKey(), asset))
                    assetsAdded++;
            }
        }
        logger.info("Environment seeding complete: {} environments and {} assets added", environmentsAdded, assetsAdded);
    }

    private boolean seedEnvironment(String id, Environment environment)
    {
        if (this.repository.findByIdOptional(id).isPresent())
        {
            logger.debug("Environment {} already present, skipping", id);
            return false;
        }
        EnvironmentEntity entity = new EnvironmentEntity();
        entity.id = id;
        entity.name = environment.name();
        this.repository.persist(entity);
        logger.debug("Seeded missing environment {}", id);
        return true;
    }

    private boolean seedAsset(String environmentId, AssetProperties asset)
    {
        if (this.assetRepository.findByIdOptional(asset.id()).isPresent())
        {
            logger.debug("Asset {} already present, skipping", asset.id());
            return false;
        }
        this.assetRepository.persist(AssetEntity.from(environmentId, asset));
        logger.debug("Seeded missing asset {} in environment {}", asset.id(), environmentId);
        return true;
    }
}
