package tech.illuin.wombat.profile;

import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.profile.persistence.ProfileData;
import tech.illuin.wombat.profile.persistence.ProfileEntity;
import tech.illuin.wombat.profile.persistence.ProfileRepository;

@ApplicationScoped
public class ProfileSeeder
{

    static final int STARTUP_PRIORITY_SEED = 2000;

    private static final Logger logger = LoggerFactory.getLogger(ProfileSeeder.class);

    private final ProfileRepository repository;
    private final ProfileSeedProperties seedProperties;

    public ProfileSeeder(ProfileRepository repository, ProfileSeedProperties seedProperties)
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

    private static ProfileEntity toEntity(ProfileSeedProperties.ProfileSeed seed)
    {
        ProfileEntity entity = new ProfileEntity();
        entity.id = seed.id();
        entity.description = seed.description();
        entity.type = seed.type();
        entity.provider = seed.provider();
        entity.location = seed.location();
        entity.data = toData(seed);
        return entity;
    }

    private static ProfileData toData(ProfileSeedProperties.ProfileSeed seed)
    {
        return switch (seed.type())
        {
            case INFRASTRUCTURE -> new ProfileData.InfrastructureData(
                seed.instanceType().orElseThrow(() -> missingField(seed, "instance-type")),
                seed.lifespan().orElseThrow(() -> missingField(seed, "lifespan"))
            );
            case LLM -> new ProfileData.LLMData(
                seed.model().orElseThrow(() -> missingField(seed, "model")),
                seed.outputTokenCount().orElseThrow(() -> missingField(seed, "output-token-count")),
                seed.requestPerYear().orElseThrow(() -> missingField(seed, "request-per-year"))
            );
        };
    }

    private static IllegalStateException missingField(ProfileSeedProperties.ProfileSeed seed, String field)
    {
        return new IllegalStateException("Profile seed " + seed.id() + " of type " + seed.type() + " is missing required field '" + field + "'");
    }
}
