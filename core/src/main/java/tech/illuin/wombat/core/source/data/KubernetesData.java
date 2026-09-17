package tech.illuin.wombat.core.source.data;

import tech.illuin.wombat.core.asset.ServiceFamily;

public record KubernetesData(
    String serviceId,
    String assetId,
    String environmentId,
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
