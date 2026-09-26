package tech.illuin.wombat.module.kubernetes_simulated;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.activity.kubernetes.KubernetesActivityData;
import tech.illuin.wombat.core.asset.AssetType;
import tech.illuin.wombat.core.asset.SimulatedAsset;

public record KubernetesSimulatedAsset(
    @NotNull @Valid KubernetesActivityData activity,
    @NotNull @JsonProperty("profile") KubernetesSimulatedProfile profile
) implements SimulatedAsset {
    public AssetType type()
    {
        return KubernetesSimulatedModule.TYPE;
    }
}
