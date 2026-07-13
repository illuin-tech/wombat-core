package tech.illuin.wombat.ui;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tech.illuin.wombat.monitor.AssetType;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = KubernetesAssetImpact.class, name = "KUBERNETES_API"),
    @JsonSubTypes.Type(value = LLMAssetImpact.class, name = "LLM_STATIC")
})
public interface AssetImpact
{
    @JsonProperty("type")
    AssetType type();

    String name();
}
