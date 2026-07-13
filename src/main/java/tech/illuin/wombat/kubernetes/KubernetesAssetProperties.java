package tech.illuin.wombat.kubernetes;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import tech.illuin.wombat.monitor.AssetProperties;
import tech.illuin.wombat.monitor.AssetType;

import java.time.Duration;
import java.util.Optional;

@RegisterForReflection
public record KubernetesAssetProperties(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("profile-id") String profileId,
    @JsonProperty("config-path") String configPath,
    @JsonProperty("namespace") String namespace,
    @JsonProperty("context") Optional<String> context,
    @JsonProperty("read-timeout") Optional<Duration> readTimeout
) implements AssetProperties
{
    @Override
    public AssetType type()
    {
        return AssetType.KUBERNETES_API;
    }
}
