package tech.illuin.wombat.persistence.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = MetricData.KubernetesData.class, name = "KUBERNETES_API")
})
public sealed interface MetricData
{
    record KubernetesData(
        String cluster,
        String namespace,
        String pod,
        String container
    ) implements MetricData {}
}
