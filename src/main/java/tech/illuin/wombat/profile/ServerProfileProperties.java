package tech.illuin.wombat.profile;

import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactRequest;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "profile.server")
public interface ServerProfileProperties
{
    boolean enable();

    @WithDefault("aws")
    BoaviztaInstanceImpactRequest.Provider provider();

    @WithDefault("a1.4xlarge")
    String instanceType();

    @WithDefault("FRA")
    String location();

    @WithDefault("43800") // 5 years
    int lifespan();
}
