package tech.illuin.wombat.asset.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tech.illuin.wombat.monitor.AssetProperties;
import tech.illuin.wombat.monitor.AssetType;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = KubernetesAsset.class, name = "KUBERNETES_API"),
    @JsonSubTypes.Type(value = LLMAsset.class, name = "LLM_STATIC")
})
public interface Asset
{
    AssetType type();

    String environmentId();

    String environmentName();

    String name();

    AssetProperties properties();
}
