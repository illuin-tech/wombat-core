package tech.illuin.wombat.profile;

import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ProfileSeeder
{

    static final int STARTUP_PRIORITY_SEED = 2000;

    private static final Logger logger = LoggerFactory.getLogger(ProfileSeeder.class);

    private final ServerProfileRepository repository;
    private final ProfileSeedProperties seedProperties;

    public ProfileSeeder(ServerProfileRepository repository, ProfileSeedProperties seedProperties)
    {
        this.repository = repository;
        this.seedProperties = seedProperties;
    }

    @Transactional
    void onStart(@Observes @Priority(STARTUP_PRIORITY_SEED) StartupEvent event)
    {
        int added = 0;
        for (ProfileSeedProperties.ProfileSeed seed : this.seedProperties.seeds())
        {
            if (this.repository.findByIdOptional(seed.id()).isPresent())
            {
                logger.debug("Profile seed {} already present, skipping", seed.id());
                continue;
            }
            this.repository.persist(toEntity(seed));
            added++;
            logger.debug("Seeded missing profile {}", seed.id());
        }
        logger.info("Profile seeding complete: {} added, {} already present",
            added, this.seedProperties.seeds().size() - added);
    }

    private static ServerProfileEntity toEntity(ProfileSeedProperties.ProfileSeed seed)
    {
        ServerProfileEntity entity = new ServerProfileEntity();
        entity.id = seed.id();
        entity.description = seed.description();
        entity.provider = seed.provider();
        entity.instanceType = seed.instanceType();
        entity.location = seed.location();
        entity.lifespan = seed.lifespan();
        return entity;
    }
}
