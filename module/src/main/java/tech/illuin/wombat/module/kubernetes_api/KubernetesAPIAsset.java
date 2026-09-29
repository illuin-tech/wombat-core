package tech.illuin.wombat.module.kubernetes_api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.source.Monitorable;

import java.time.Duration;
import java.util.Optional;

public record KubernetesAPIAsset(
    @JsonUnwrapped AssetIdentity identity,
    @NotBlank @JsonProperty("config-path") String configPath,
    @JsonProperty("namespace") String namespace,
    @JsonProperty("context") Optional<String> context,
    @JsonProperty("read-timeout") Optional<Duration> readTimeout,
    @JsonProperty("heartbeat-skip") int heartbeatSkip,
    @NotNull @JsonProperty("profile") KubernetesAPIServerProfile profile
) implements Asset, Monitorable
{
    @Override
    public AssetType type()
    {
        return KubernetesAPIModule.TYPE;
    }
}
