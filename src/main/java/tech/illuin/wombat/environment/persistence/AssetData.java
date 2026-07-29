package tech.illuin.wombat.environment.persistence;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tech.illuin.wombat.asset.model.profile.DynamicLLMProfile;
import tech.illuin.wombat.asset.model.profile.InfrastructureProfile;
import tech.illuin.wombat.asset.model.profile.StaticLLMProfile;

import java.time.Duration;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AssetData.KubernetesAPIData.class, name = "KUBERNETES_API"),
    @JsonSubTypes.Type(value = AssetData.LLMStaticData.class, name = "LLM_STATIC"),
    @JsonSubTypes.Type(value = AssetData.LLMPrometheusData.class, name = "LLM_PROMETHEUS")
})
public sealed interface AssetData
{
    record KubernetesAPIData(
        @JsonProperty("config_path") String configPath,
        @JsonProperty("namespace") String namespace,
        @JsonProperty("context") String context,
        @JsonProperty("read_timeout") Duration readTimeout,
        @JsonProperty("heartbeat_skip") int heartbeatSkip,
        @JsonProperty("profile") InfrastructureProfile profile
    ) implements AssetData {}

    record LLMStaticData(
        @JsonProperty("profile") StaticLLMProfile profile
    ) implements AssetData {}

    record LLMPrometheusData(
        @JsonProperty("prometheus_url") String prometheusUrl,
        @JsonProperty("proxy_url") String proxyUrl,
        @JsonProperty("username") String username,
        @JsonProperty("password") String password,
        @JsonProperty("heartbeat_skip") int heartbeatSkip,
        @JsonProperty("profile") DynamicLLMProfile profile
    ) implements AssetData {}
}
