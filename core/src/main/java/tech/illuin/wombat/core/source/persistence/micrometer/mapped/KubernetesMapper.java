package tech.illuin.wombat.core.source.persistence.micrometer.mapped;

import io.micrometer.core.instrument.Tag;
import tech.illuin.wombat.core.source.data.KubernetesData;
import tech.illuin.wombat.core.source.data.MetricData;
import tech.illuin.wombat.core.source.persistence.micrometer.mapped.MappedMicrometerMetricPersister.Value;

import java.util.List;

import static tech.illuin.wombat.core.source.persistence.micrometer.MicrometerTags.*;
import static tech.illuin.wombat.core.source.persistence.micrometer.MicrometerTags.TAG_K8S_CLUSTER;
import static tech.illuin.wombat.core.source.persistence.micrometer.MicrometerTags.TAG_K8S_CONTAINER;
import static tech.illuin.wombat.core.source.persistence.micrometer.MicrometerTags.TAG_K8S_NAMESPACE;
import static tech.illuin.wombat.core.source.persistence.micrometer.MicrometerTags.TAG_K8S_POD;

public class KubernetesMapper implements MappedMicrometerMetricPersister.Mapper
{
    @Override
    public List<Tag> mapTags(MetricData data)
    {
        KubernetesData k8sData = asKubernetesData(data);

        return List.of(
            Tag.of(TAG_ENVIRONMENT, data.environmentId()),
            Tag.of(TAG_ASSET, data.assetId()),
            Tag.of(TAG_SERVICE, data.serviceId()),
            Tag.of(TAG_K8S_CLUSTER, k8sData.cluster()),
            Tag.of(TAG_K8S_NAMESPACE, k8sData.namespace()),
            Tag.of(TAG_K8S_POD, k8sData.pod()),
            Tag.of(TAG_K8S_CONTAINER, data.serviceId())
        );
    }

    @Override
    public List<Value> mapValues(MetricData data)
    {
        KubernetesData k8sData = asKubernetesData(data);

        return List.of(
            new Value(METRIC_K8S_CPU, k8sData.cpuNanocores()),
            new Value(METRIC_K8S_RAM, k8sData.ramBytes())
        );
    }

    private static KubernetesData asKubernetesData(MetricData data)
    {
        if (data instanceof KubernetesData k8sData)
            return k8sData;
        throw new IllegalArgumentException("Data must be of type KubernetesData instead of provided " + data.getClass().getSimpleName());
    }
}
