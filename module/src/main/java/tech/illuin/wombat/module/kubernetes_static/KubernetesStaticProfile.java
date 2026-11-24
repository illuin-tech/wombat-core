package tech.illuin.wombat.module.kubernetes_static;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.Lifetime;
import tech.illuin.wombat.core.asset.profile.CloudServerProfile;
import tech.illuin.wombat.core.asset.profile.ServerProvider;
import tech.illuin.wombat.module.kubernetes_static.scenario.Scenario;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

@JsonClassDescription("Hardware of the cluster's nodes and their modeled load")
public record KubernetesStaticProfile(
    @NotNull @JsonPropertyDescription("Infrastructure provider") ServerProvider provider,
    @NotBlank @JsonPropertyDescription("Instance type of the nodes, e.g. c5.2xlarge or n2-standard-8") String instanceType,
    @NotNull @JsonPropertyDescription("Country hosting the nodes") Country location,
    @NotNull @JsonPropertyDescription("Expected operational lifespan of the hardware") Lifetime lifespan,
    @Positive @JsonProperty(defaultValue = "1") @JsonPropertyDescription("Number of identical nodes") int nodeCount,
    @JsonProperty(defaultValue = "UTC") @JsonPropertyDescription("Time zone the scenarios' times are evaluated in, e.g. Europe/Paris") ZoneId zoneId,
    @NotNull @NotEmpty @JsonPropertyDescription("Superposed load scenarios") List<@Valid Scenario> scenarios
) implements CloudServerProfile
{
    public KubernetesStaticProfile
    {
        if (nodeCount <= 0)
            nodeCount = 1;
        zoneId = zoneId != null ? zoneId : ZoneOffset.UTC;
        scenarios = scenarios != null ? List.copyOf(scenarios) : List.of();
    }

    public KubernetesStaticProfile(
        ServerProvider provider,
        String instanceType,
        Country location,
        Lifetime lifespan,
        ZoneId zoneId,
        List<Scenario> scenarios
    )
    {
        this(provider, instanceType, location, lifespan, 1, zoneId, scenarios);
    }
}
