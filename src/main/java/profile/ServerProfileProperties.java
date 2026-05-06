package profile;

import boavizta.model.BoaviztaInstanceImpactRequest;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "profile.server")
public interface ServerProfileProperties {
    boolean enable();

    @WithDefault("aws")
    BoaviztaInstanceImpactRequest.Provider provider();

    @WithDefault("a1.4xlarge")
    String instance_type();

    @WithDefault("FRA")
    String location();

    @WithDefault("5000")
    int lifespan();
}
