package tech.illuin.wombat.profile;

import io.smallrye.config.ConfigMapping;
import tech.illuin.wombat.profile.model.ProfileType;

import java.util.List;
import java.util.Optional;

@ConfigMapping(prefix = "profiles")
public interface ProfileSeedProperties
{

    List<ProfileSeed> seeds();

    interface ProfileSeed
    {

        String id();

        String description();

        ProfileType type();

        String provider();

        String location();

        Optional<String> instanceType();

        Optional<Integer> lifespan();

        Optional<String> model();

        Optional<Integer> outputTokenCount();

        Optional<Integer> requestPerYear();
    }
}
