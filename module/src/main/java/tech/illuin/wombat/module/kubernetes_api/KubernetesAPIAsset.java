package tech.illuin.wombat.module.kubernetes_api;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.source.Monitorable;
import tech.illuin.wombat.core.source.Period;

import java.time.Duration;
import java.util.Optional;

public record KubernetesAPIAsset(
    @JsonUnwrapped AssetIdentity identity,
    @NotBlank @JsonProperty("configPath") @JsonAlias("config-path") String configPath,
    @JsonProperty("namespace") String namespace,
    @JsonProperty("context") Optional<String> context,
    @JsonProperty("readTimeout") @JsonAlias("read-timeout") Optional<Duration> readTimeout,
    @JsonProperty("period") Period period,
    @NotNull @JsonProperty("profile") KubernetesAPIServerProfile profile
) implements Asset, Monitorable
{
    @Override
    public AssetType type()
    {
        return KubernetesAPIModule.TYPE;
    }
}
