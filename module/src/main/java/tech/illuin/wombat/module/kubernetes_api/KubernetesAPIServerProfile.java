package tech.illuin.wombat.module.kubernetes_api;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.core.asset.profile.ServerProfile;
import tech.illuin.wombat.core.asset.profile.ServerProvider;

public record KubernetesAPIServerProfile(
    @JsonProperty("provider") ServerProvider provider,
    @JsonProperty("instance-type") String instanceType,
    @JsonProperty("location") String location,
    @JsonProperty("lifespan") int lifespan
) implements ServerProfile {}
