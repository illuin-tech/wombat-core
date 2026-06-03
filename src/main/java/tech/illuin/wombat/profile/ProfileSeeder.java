package tech.illuin.wombat.profile;

import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ProfileSeeder
{

    static final int STARTUP_PRIORITY_SEED = 2000;

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
        if (this.repository.count() > 0)
            return;
        for (ProfileSeedProperties.ProfileSeed seed : this.seedProperties.seeds())
        {
            ServerProfileEntity entity = new ServerProfileEntity();
            entity.id = seed.id();
            entity.description = seed.description();
            entity.provider = seed.provider();
            entity.instanceType = seed.instanceType();
            entity.location = seed.location();
            entity.lifespan = seed.lifespan();
            this.repository.persist(entity);
        }
    }
}
