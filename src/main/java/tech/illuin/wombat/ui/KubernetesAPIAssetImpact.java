package tech.illuin.wombat.ui;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceConfigResponse;
import tech.illuin.wombat.handler.model.KubernetesImpactResponse;
import tech.illuin.wombat.monitor.AssetType;

public record KubernetesAPIAssetImpact(
    @JsonProperty("name") String name,
    @JsonProperty("response") KubernetesImpactResponse response,
    @JsonProperty("instance_config") BoaviztaInstanceConfigResponse instanceConfig,
    @JsonProperty("load_percent") double loadPercent,
    @JsonProperty("node_count") int nodeCount
) implements AssetImpact
{
    @Override
    public AssetType type()
    {
        return AssetType.KUBERNETES_API;
    }
}
