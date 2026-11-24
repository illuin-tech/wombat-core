package tech.illuin.wombat.module.kubernetes_api;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.CloudServerProfile;
import tech.illuin.wombat.core.asset.profile.Lifetime;
import tech.illuin.wombat.core.asset.profile.ServerProvider;

@JsonClassDescription("Cloud instances backing the cluster's nodes")
public record KubernetesAPICloudProfile(
    @NotNull @JsonPropertyDescription("Infrastructure provider") ServerProvider provider,
    // The kebab-case @JsonAlias read assets persisted before 0.15, when properties were named that way
    @NotBlank @JsonPropertyDescription("Instance type of the nodes, e.g. m5.xlarge or n2-standard-4") @JsonAlias("instance-type") String instanceType,
    @NotNull @JsonPropertyDescription("Country hosting the nodes") Country location,
    @NotNull @JsonPropertyDescription("Expected operational lifespan of the hardware") Lifetime lifespan
) implements KubernetesAPIServerProfile, CloudServerProfile {}
