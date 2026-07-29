package tech.illuin.wombat.persistence.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = MetricData.KubernetesData.class, name = "KUBERNETES_API"),
    @JsonSubTypes.Type(value = MetricData.LLMData.class, name = "LLM")
})
public sealed interface MetricData
{
    record KubernetesData(
        String cluster,
        String namespace,
        String pod,
        String container
    ) implements MetricData {}

    record LLMData(
        String profileId,
        String model
    ) implements MetricData {}
}
