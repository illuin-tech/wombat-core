package tech.illuin.wombat.module.kubernetes_simulated;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.core.activity.kubernetes.KubernetesActivityData;
import tech.illuin.wombat.core.asset.AssetType;
import tech.illuin.wombat.core.asset.SimulatedAsset;

public record KubernetesSimulatedAsset(
    KubernetesActivityData activity,
    @JsonProperty("profile") KubernetesSimulatedProfile profile
) implements SimulatedAsset {
    public AssetType type()
    {
        return AssetType.KUBERNETES_SIMULATED;
    }
}
