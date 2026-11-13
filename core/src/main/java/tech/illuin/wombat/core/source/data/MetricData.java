package tech.illuin.wombat.core.source.data;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tech.illuin.wombat.core.asset.type.ServiceFamily;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "family")
@JsonSubTypes({
    @JsonSubTypes.Type(value = KubernetesData.class, name = "KUBERNETES_CONTAINER"),
    @JsonSubTypes.Type(value = LLMData.class, name = "LLM"),
})
public interface MetricData
{
    String serviceId();

    ServiceFamily serviceFamily();
}
