package tech.illuin.wombat.profile;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactRequest;

import java.util.List;

@ConfigMapping(prefix = "profiles")
public interface ProfileSeedProperties
{

    List<ProfileSeed> seeds();

    @WithName("default")
    String defaultId();

    interface ProfileSeed
    {

        String id();

        String description();

        BoaviztaInstanceImpactRequest.Provider provider();

        String instanceType();

        String location();

        int lifespan();
    }
}
