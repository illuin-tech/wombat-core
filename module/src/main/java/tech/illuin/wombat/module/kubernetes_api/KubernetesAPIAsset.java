package tech.illuin.wombat.module.kubernetes_api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetType;
import tech.illuin.wombat.core.source.Monitorable;

import java.time.Duration;
import java.util.Optional;

public record KubernetesAPIAsset(
    @NotBlank @JsonProperty("id") String id,
    @NotBlank @JsonProperty("environment-id") String environmentId,
    @NotBlank @JsonProperty("name") String name,
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
