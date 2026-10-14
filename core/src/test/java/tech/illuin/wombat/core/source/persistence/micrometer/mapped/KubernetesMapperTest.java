package tech.illuin.wombat.core.source.persistence.micrometer.mapped;

import io.micrometer.core.instrument.Tag;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.source.data.KubernetesData;
import tech.illuin.wombat.core.source.persistence.micrometer.mapped.MappedMicrometerMetricPersister.Value;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.illuin.wombat.core.source.persistence.micrometer.MicrometerTags.*;

class KubernetesMapperTest
{
    private static final AssetType TYPE_KUBERNETES = AssetType.of("tech.illuin", "wombat-module", "kubernetes-prometheus", ActivityRegime.MEASURED, ServiceFamily.KUBERNETES_CONTAINER);
    private static final AssetIdentity IDENTITY = AssetIdentity.of("asset-1", "env-1", "Asset 1");

    private final KubernetesMapper mapper = new KubernetesMapper();

    @Test
    void mapTags_includesClusterNamespacePodContainer()
    {
        KubernetesData data = new KubernetesData("container-1", "cluster-prod", "default", "pod-abc", 500_000_000.0, 1024.0 * 1024.0 * 512.0);
        List<Tag> tags = this.mapper.mapTags(IDENTITY, TYPE_KUBERNETES, data);

        assertTrue(tags.contains(Tag.of(TAG_ENVIRONMENT, "env-1")));
        assertTrue(tags.contains(Tag.of(TAG_ASSET, "asset-1")));
        assertTrue(tags.contains(Tag.of(TAG_ASSET_NAME, "Asset 1")));
        assertTrue(tags.contains(Tag.of(TAG_ASSET_TYPE, TYPE_KUBERNETES.name())));
        assertTrue(tags.contains(Tag.of(TAG_SERVICE, "container-1")));
        assertTrue(tags.contains(Tag.of(TAG_K8S_CLUSTER, "cluster-prod")));
        assertTrue(tags.contains(Tag.of(TAG_K8S_NAMESPACE, "default")));
        assertTrue(tags.contains(Tag.of(TAG_K8S_POD, "pod-abc")));
        assertTrue(tags.contains(Tag.of(TAG_K8S_CONTAINER, "container-1")));
    }

    @Test
    void mapValues_mapsCpuAndRam()
    {
        KubernetesData data = new KubernetesData("container-1", "cluster-prod", "default", "pod-abc", 500_000_000.0, 1024.0 * 1024.0 * 512.0);
        Map<String, Double> values = this.mapper.mapValues(data).stream()
            .collect(Collectors.toMap(Value::key, Value::value));

        assertTrue(values.containsKey(METRIC_K8S_CPU));
        assertEquals(500_000_000.0, values.get(METRIC_K8S_CPU));
        assertTrue(values.containsKey(METRIC_K8S_RAM));
        assertEquals(1024.0 * 1024.0 * 512.0, values.get(METRIC_K8S_RAM));
    }
}
