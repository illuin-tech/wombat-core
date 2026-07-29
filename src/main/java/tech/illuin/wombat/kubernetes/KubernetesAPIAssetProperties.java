package tech.illuin.wombat.kubernetes;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import tech.illuin.wombat.asset.model.profile.InfrastructureProfile;
import tech.illuin.wombat.monitor.AssetProperties;
import tech.illuin.wombat.monitor.AssetType;
import tech.illuin.wombat.monitor.Monitored;

import java.time.Duration;
import java.util.Optional;

@RegisterForReflection
public record KubernetesAPIAssetProperties(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("config-path") String configPath,
    @JsonProperty("namespace") String namespace,
    @JsonProperty("context") Optional<String> context,
    @JsonProperty("read-timeout") Optional<Duration> readTimeout,
    @JsonProperty("heartbeat-skip") int heartbeatSkip,
    @JsonProperty("profile") InfrastructureProfile profile
) implements AssetProperties, Monitored
{
    @Override
    public AssetType type()
    {
        return AssetType.KUBERNETES_API;
    }
}
