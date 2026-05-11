package tech.illuin.vigilantwombat.profile;

import tech.illuin.vigilantwombat.boavizta.model.BoaviztaInstanceImpactRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ServerConfig(
    @JsonProperty("provider") BoaviztaInstanceImpactRequest.Provider provider,
    @JsonProperty("instance_type") String instanceType,
    @JsonProperty("location") String location,
    @JsonProperty("lifespan") int lifespan
) {
    public static ServerConfig fromServerProfile(ServerProfileProperties properties) {
        return new ServerConfig(properties.provider(), properties.instance_type(), properties.location(), properties.lifespan());
    }
}
