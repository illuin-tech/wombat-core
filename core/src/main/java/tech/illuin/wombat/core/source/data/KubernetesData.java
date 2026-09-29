package tech.illuin.wombat.core.source.data;

import tech.illuin.wombat.core.asset.type.ServiceFamily;

public record KubernetesData(
    String serviceId,
    String cluster,
    String namespace,
    String pod,
    double cpuNanocores,
    double ramBytes
) implements MetricData {
    @Override
    public ServiceFamily serviceFamily()
    {
        return ServiceFamily.KUBERNETES_CONTAINER;
    }
}
