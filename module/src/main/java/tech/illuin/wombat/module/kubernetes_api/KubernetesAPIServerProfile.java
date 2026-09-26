package tech.illuin.wombat.module.kubernetes_api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import tech.illuin.wombat.core.asset.profile.ServerProfile;
import tech.illuin.wombat.core.asset.profile.ServerProvider;

public record KubernetesAPIServerProfile(
    @NotNull @JsonProperty("provider") ServerProvider provider,
    @NotBlank @JsonProperty("instance-type") String instanceType,
    @NotBlank @JsonProperty("location") String location,
    @Positive @JsonProperty("lifespan") int lifespan
) implements ServerProfile {}
