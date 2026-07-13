package tech.illuin.wombat.kubernetes;

import tech.illuin.wombat.monitor.AssetProperties;
import tech.illuin.wombat.monitor.AssetType;
import tech.illuin.wombat.monitor.MonitoredAssetHandler;

import java.time.Instant;

public class KubernetesMonitorHandler implements MonitoredAssetHandler
{
    private final KubernetesMetricsCollector metricsCollector;

    public KubernetesMonitorHandler(KubernetesMetricsCollector metricsCollector)
    {
        this.metricsCollector = metricsCollector;
    }

    @Override
    public boolean accept(AssetProperties config) {
        return config.type() == AssetType.KUBERNETES_API;
    }

    @Override
    public void handle(Instant instant, AssetProperties config)
    {
        if (!(config instanceof KubernetesAssetProperties clusterConfig))
            throw new IllegalArgumentException("Expected KubernetesAssetProperties config not provided");
        this.metricsCollector.listPods(instant, clusterConfig);
    }
}
