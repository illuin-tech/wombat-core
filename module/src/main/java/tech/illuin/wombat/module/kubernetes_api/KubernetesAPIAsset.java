package tech.illuin.wombat.module.kubernetes_api;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetType;
import tech.illuin.wombat.core.source.Monitorable;

import java.time.Duration;
import java.util.Optional;

public record KubernetesAPIAsset(
    @JsonProperty("id") String id,
    @JsonProperty("environment-id") String environmentId,
    @JsonProperty("name") String name,
    @JsonProperty("config-path") String configPath,
    @JsonProperty("namespace") String namespace,
    @JsonProperty("context") Optional<String> context,
    @JsonProperty("read-timeout") Optional<Duration> readTimeout,
    @JsonProperty("heartbeat-skip") int heartbeatSkip,
    @JsonProperty("profile") KubernetesAPIServerProfile profile
) implements Asset, Monitorable
{
    @Override
    public AssetType type()
    {
        return AssetType.KUBERNETES_API;
    }
}
