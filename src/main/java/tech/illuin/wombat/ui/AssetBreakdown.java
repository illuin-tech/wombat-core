package tech.illuin.wombat.ui;

import tech.illuin.wombat.boavizta.model.BoaviztaInstanceConfigResponse;
import tech.illuin.wombat.handler.model.KubernetesImpactResponse;
import tech.illuin.wombat.handler.model.ProviderConfig;
import tech.illuin.wombat.monitor.AssetType;
import tech.illuin.wombat.model.Footprint;
import tech.illuin.wombat.profile.model.MeasureType;

import java.util.List;

public record AssetBreakdown(
    String name,
    MeasureType measureType,
    ProviderConfig providerConfig,
    BoaviztaInstanceConfigResponse instanceConfig,
    List<String> services,
    boolean container,
    Load load
)
{
    public static AssetBreakdown from(KubernetesAssetImpact asset)
    {
        KubernetesImpactResponse response = asset.response();
        List<String> services = response.serviceImpacts().stream().map(Footprint::service).toList();
        boolean container = response.providerConfig().datasource() == AssetType.KUBERNETES_API;
        return new AssetBreakdown(
            asset.name(),
            asset.measureType(),
            response.providerConfig(),
            asset.instanceConfig(),
            services,
            container,
            new Load(response.cpuUsageCores(), asset.loadPercent(), asset.nodeCount())
        );
    }

    public record Load(double cpuUsageCores, double loadPercent, int nodeCount) {}
}
