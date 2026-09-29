package tech.illuin.wombat.core.source.data;

import tech.illuin.wombat.core.asset.type.ServiceFamily;

import java.util.Objects;

public record KubernetesData(
    String serviceId,
    String cluster,
    String namespace,
    String pod,
    double cpuNanocores,
    double ramBytes
) implements MetricData {
    public KubernetesData {
        serviceId = Objects.requireNonNull(serviceId);
        cluster = Objects.requireNonNull(cluster);
        namespace = Objects.requireNonNull(namespace);
        pod = Objects.requireNonNull(pod);
    }

    @Override
    public ServiceFamily serviceFamily()
    {
        return ServiceFamily.KUBERNETES_CONTAINER;
    }
}
