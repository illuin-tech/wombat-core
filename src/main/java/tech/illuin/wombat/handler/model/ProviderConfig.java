package tech.illuin.wombat.handler.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tech.illuin.wombat.monitor.AssetType;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = BoaviztaKubernetesConfig.class, name = "BOAVIZTA_KUBERNETES")
})
public sealed interface ProviderConfig permits BoaviztaKubernetesConfig
{

    AssetType datasource();

    int lifespan();
}
