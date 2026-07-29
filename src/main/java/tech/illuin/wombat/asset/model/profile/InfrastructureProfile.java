package tech.illuin.wombat.asset.model.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactRequest;

public record InfrastructureProfile(
    @JsonProperty("provider") BoaviztaInstanceImpactRequest.Provider provider,
    @JsonProperty("instance-type") String instanceType,
    @JsonProperty("location") String location,
    @JsonProperty("lifespan") int lifespan
) {}
