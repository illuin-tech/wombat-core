package tech.illuin.wombat.ui;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tech.illuin.wombat.monitor.AssetType;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = KubernetesAPIAssetImpact.class, name = "KUBERNETES_API"),
    @JsonSubTypes.Type(value = LLMStaticAssetImpact.class, name = "LLM_STATIC"),
    @JsonSubTypes.Type(value = LLMPrometheusAssetImpact.class, name = "LLM_PROMETHEUS")
})
public interface AssetImpact
{
    @JsonProperty("type")
    AssetType type();

    @JsonProperty("measure_type")
    default String measureType()
    {
        return this.type().measureLabel();
    }

    String name();
}
