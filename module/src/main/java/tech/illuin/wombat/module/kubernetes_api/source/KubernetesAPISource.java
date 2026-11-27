package tech.illuin.wombat.module.kubernetes_api.source;

import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.ContainerMetrics;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.secret.MissingSecretException;
import tech.illuin.wombat.core.source.WombatSource;
import tech.illuin.wombat.core.source.WombatSourceException;
import tech.illuin.wombat.core.source.data.KubernetesData;
import tech.illuin.wombat.core.source.data.MetricData;
import tech.illuin.wombat.module.kubernetes_api.KubernetesAPIAsset;
import tech.illuin.wombat.module.kubernetes_api.connector.KubernetesClientSupplier;

import java.net.HttpURLConnection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KubernetesAPISource implements WombatSource, AutoCloseable
{
    private final KubernetesClientSupplier clients;

    private static final Logger logger = LoggerFactory.getLogger(KubernetesAPISource.class);

    public KubernetesAPISource(KubernetesClientSupplier clients)
    {
        this.clients = clients;
    }

    @Override
    public boolean accept(Asset asset)
    {
        return asset instanceof KubernetesAPIAsset;
    }

    @Override
    public List<MetricData> source(Instant heartbeat, Instant lastActivation, Asset asset) throws WombatSourceException
    {
        KubernetesAPIAsset kubernetesAsset = (KubernetesAPIAsset) asset;

        logger.trace("Persisting pods usages for kubernetes config {}", asset.identity().id());

        KubernetesClient client;
        try {
            client = this.clients.get();
        }
        /* The kubeconfig was unbound, or rotated to one that cannot be read or lacks the asset's context. */
        catch (MissingSecretException | IllegalArgumentException e) {
            throw new WombatSourceException("Cannot build a Kubernetes client for asset " + asset.identity().id() + ": " + e.getMessage(), e);
        }

        try {
            return this.outputToTarget(this.fetchPodMetrics(client, kubernetesAsset), kubernetesAsset);
        }
        catch (KubernetesClientException e) {
            if (e.getCode() == HttpURLConnection.HTTP_FORBIDDEN)
                throw new WombatSourceException("Cannot list pod metrics cluster-wide for asset " + asset.identity().id() + ": restrict namespaces to the ones its kubeconfig can read", e);
            throw new WombatSourceException("Cannot list pod metrics for asset " + asset.identity().id() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void close()
    {
        this.clients.close();
    }

    private List<PodMetrics> fetchPodMetrics(KubernetesClient client, KubernetesAPIAsset asset)
    {
        if (asset.namespaces().isEmpty())
            return client.top().pods().metrics().getItems();

        List<PodMetrics> podMetricsList = new ArrayList<>();
        for (String namespace : asset.namespaces())
        {
            try {
                podMetricsList.addAll(client.top().pods().metrics(namespace).getItems());
            }
            catch (KubernetesClientException e) {
                logger.warn("Cannot list pod metrics in namespace {} for asset {}, skipping it: {}", namespace, asset.identity().id(), e.getMessage());
            }
        }
        return podMetricsList;
    }

    private List<MetricData> outputToTarget(List<PodMetrics> podMetricsList, KubernetesAPIAsset properties)
    {
        String clusterId = properties.identity().id();

        logger.trace("Recording metrics for {} pods in {} {}", podMetricsList.size(), clusterId, properties.namespaces());
        List<MetricData> metricData = new ArrayList<>();
        for (PodMetrics podMetrics : podMetricsList)
        {
            String namespace = podMetrics.getMetadata().getNamespace();
            String podName = podMetrics.getMetadata().getName();
            for (ContainerMetrics cm : podMetrics.getContainers())
            {
                logger.trace("Current metrics {}", cm);
                Map<String, Quantity> usage = cm.getUsage();
                metricData.add(new KubernetesData(
                    cm.getName(),
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
