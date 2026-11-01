package tech.illuin.wombat.module.kubernetes_static;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.Lifetime;
import tech.illuin.wombat.core.asset.profile.ServerProfile;
import tech.illuin.wombat.core.asset.profile.ServerProvider;
import tech.illuin.wombat.module.kubernetes_static.scenario.Scenario;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

public record KubernetesStaticProfile(
    @NotNull @JsonProperty("provider") ServerProvider provider,
    @NotBlank @JsonProperty("instanceType") @JsonAlias("instance-type") String instanceType,
    @NotNull @JsonProperty("location") Country location,
    @NotNull @JsonProperty("lifespan") Lifetime lifespan,
    @Positive @JsonProperty("nodeCount") @JsonAlias("node-count") Integer nodeCount,
    @JsonProperty("zoneId") @JsonAlias("zone-id") ZoneId zoneId,
    @NotNull @NotEmpty @JsonProperty("scenarios") List<@Valid Scenario> scenarios
) implements ServerProfile
{
    public KubernetesStaticProfile
    {
        nodeCount = nodeCount != null ? nodeCount : 1;
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
