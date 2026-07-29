package tech.illuin.wombat.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import tech.illuin.wombat.asset.model.profile.DynamicLLMProfile;
import tech.illuin.wombat.monitor.AssetProperties;
import tech.illuin.wombat.monitor.AssetType;
import tech.illuin.wombat.monitor.Monitored;

@RegisterForReflection
public record LLMPrometheusProperties(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("prometheus-url") String prometheusUrl,
    @JsonProperty("proxy-url") String proxyUrl,
    @JsonProperty("username") String username,
    @JsonProperty("password") String password,
    @JsonProperty("heartbeat-skip") int heartbeatSkip,
    @JsonProperty("profile") DynamicLLMProfile profile
) implements AssetProperties, Monitored
{
    @Override
    public AssetType type()
    {
        return AssetType.LLM_PROMETHEUS;
    }
}
