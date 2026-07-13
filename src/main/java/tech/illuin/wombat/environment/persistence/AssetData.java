package tech.illuin.wombat.environment.persistence;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Duration;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AssetData.KubernetesData.class, name = "KUBERNETES_API"),
    @JsonSubTypes.Type(value = AssetData.LLMData.class, name = "LLM_STATIC")
})
public sealed interface AssetData
{
    record KubernetesData(
        @JsonProperty("config_path") String configPath,
        @JsonProperty("namespace") String namespace,
        @JsonProperty("context") String context,
        @JsonProperty("read_timeout") Duration readTimeout
    ) implements AssetData {}

    record LLMData() implements AssetData {}
}
