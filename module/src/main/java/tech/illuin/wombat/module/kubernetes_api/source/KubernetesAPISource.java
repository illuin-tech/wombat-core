package tech.illuin.wombat.module.kubernetes_api.source;

import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.ContainerMetrics;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.source.WombatSource;
import tech.illuin.wombat.core.source.data.KubernetesData;
import tech.illuin.wombat.core.source.data.MetricData;
import tech.illuin.wombat.module.kubernetes_api.KubernetesAPIAsset;
import tech.illuin.wombat.module.kubernetes_api.connector.KubernetesMultiClusterClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KubernetesAPISource implements WombatSource
{
    private final KubernetesMultiClusterClient multiClusterApi;

    private static final Logger logger = LoggerFactory.getLogger(KubernetesAPISource.class);

    public KubernetesAPISource(KubernetesMultiClusterClient multiClusterApi)
    {
        this.multiClusterApi = multiClusterApi;
    }

    @Override
    public boolean accept(Asset asset)
    {
        return asset instanceof KubernetesAPIAsset;
    }

    @Override
    public List<MetricData> source(Instant heartbeat, Asset asset)
    {
        KubernetesAPIAsset kubernetesAsset = (KubernetesAPIAsset) asset;

        String clusterId = asset.id();
        logger.trace("Persisting pods usages for kubernetes config {}", asset.id());

        List<PodMetrics> podMetricsList = this.multiClusterApi.get(asset.id())
            .orElseThrow(() -> new IllegalStateException("No client registered for cluster " + clusterId))
            .top()
            .pods()
            .inNamespace(kubernetesAsset.namespace())
            .metrics()
            .getItems();

        return this.outputToTarget(podMetricsList, asset.environmentId(), kubernetesAsset);
    }

    private List<MetricData> outputToTarget(List<PodMetrics> podMetricsList, String environmentId, KubernetesAPIAsset properties)
    {
        String clusterId = properties.id();
        String namespace = properties.namespace();
        String assetId = properties.id();

        logger.trace("Recording metrics for {} pods in {}/{}", podMetricsList.size(), clusterId, namespace);
        List<MetricData> metricData = new ArrayList<>();
        for (PodMetrics podMetrics : podMetricsList)
        {
            String podName = podMetrics.getMetadata().getName();
            for (ContainerMetrics cm : podMetrics.getContainers())
            {
                logger.trace("Current metrics {}", cm);
                Map<String, Quantity> usage = cm.getUsage();
                metricData.add(new KubernetesData(
                    cm.getName(),
                    assetId,
                    environmentId,
                    clusterId,
                    namespace,
                    podName,
                    readCpu(usage, podName, cm.getName()),
                    readMemory(usage, podName, cm.getName())
                ));
            }
        }
        return metricData;
    }

    private static double readCpu(Map<String, Quantity> usage, String pod, String container)
    {
        if (usage == null || usage.get("cpu") == null)
        {
            logger.warn("No CPU metric for container {} in pod {}, recording 0", container, pod);
            return 0.0;
        }
        String amount = usage.get("cpu").getAmount();
        try {
            return Double.parseDouble(amount);
        }
        catch (NumberFormatException e) {
            logger.warn("CPU value {} for container {} in pod {} is not a parseable double, recording 0", amount, container, pod);
            return 0.0;
        }
    }

    private static double readMemory(Map<String, Quantity> usage, String pod, String container)
    {
        if (usage == null || usage.get("memory") == null)
        {
            logger.warn("No memory metric for container {} in pod {}, recording 0", container, pod);
            return 0.0;
        }
        try {
            return Quantity.getAmountInBytes(usage.get("memory")).doubleValue();
        }
        catch (IllegalArgumentException e) {
            logger.warn("Memory value {} for container {} in pod {} is not parseable, recording 0", usage.get("memory"), container, pod);
            return 0.0;
        }
    }
}
