package tech.illuin.wombat.module.kubernetes_api;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.BareServerProfile;
import tech.illuin.wombat.core.asset.profile.Lifetime;

@JsonClassDescription("Bare servers backing the cluster's nodes, specified component by component")
public record KubernetesAPIBareServerProfile(
    @NotNull @JsonPropertyDescription("Country hosting the nodes") Country location,
    @NotNull @JsonPropertyDescription("Expected operational lifespan of the hardware") Lifetime lifespan,
    @NotNull @Valid @JsonPropertyDescription("Processors of a node") CPU cpu,
    @NotNull @Valid @JsonPropertyDescription("Memory of a node") RAM ram,
    @Valid @JsonPropertyDescription("SSDs of a node, Boavizta's default (one 1 TB SSD) when omitted") Disk ssd,
    @Valid @JsonPropertyDescription("HDDs of a node, none when omitted") Disk hdd,
    @Valid @JsonPropertyDescription("GPUs of a node, none when omitted") GPU gpu
) implements KubernetesAPIServerProfile, BareServerProfile {}
