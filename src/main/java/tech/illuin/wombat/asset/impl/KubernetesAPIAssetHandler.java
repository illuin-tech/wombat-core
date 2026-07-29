package tech.illuin.wombat.asset.impl;

import tech.illuin.wombat.asset.model.Asset;
import tech.illuin.wombat.asset.model.KubernetesAPIAsset;
import tech.illuin.wombat.asset.AssetHandler;
import tech.illuin.wombat.boavizta.BoaviztaClient;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceConfigResponse;
import tech.illuin.wombat.handler.KubernetesImpactService;
import tech.illuin.wombat.handler.model.KubernetesImpactResponse;
import tech.illuin.wombat.monitor.AssetProperties;
import tech.illuin.wombat.monitor.AssetType;
import tech.illuin.wombat.persistence.NoCPUUsageException;
import tech.illuin.wombat.persistence.model.TimeRange;
import tech.illuin.wombat.asset.model.profile.InfrastructureProfile;
import tech.illuin.wombat.ui.KubernetesAPIAssetImpact;

import java.util.Collection;
import java.util.List;

public class KubernetesAPIAssetHandler implements AssetHandler<KubernetesAPIAssetImpact>
{
    private final KubernetesImpactService kubernetesImpactService;
    private final BoaviztaClient boaviztaClient;

    public KubernetesAPIAssetHandler(KubernetesImpactService kubernetesImpactService, BoaviztaClient boaviztaClient)
    {
        this.kubernetesImpactService = kubernetesImpactService;
        this.boaviztaClient = boaviztaClient;
    }

    @Override
    public boolean accept(AssetProperties assetProperties)
    {
        return assetProperties.type() == AssetType.KUBERNETES_API;
    }

    @Override
    public KubernetesAPIAssetImpact computeImpact(Asset asset, Collection<String> assetServices, TimeRange timeRange) throws NoCPUUsageException
    {
        KubernetesAPIAsset kubernetesAsset = (KubernetesAPIAsset) asset;
        KubernetesImpactResponse response = this.kubernetesImpactService.computeImpactResponse(timeRange, assetServices, List.of(kubernetesAsset)).getFirst();
        InfrastructureProfile profile = kubernetesAsset.profile();
        BoaviztaInstanceConfigResponse instanceConfig = this.boaviztaClient.getInstanceConfig(profile.provider(), profile.instanceType());
        int vcpu = instanceConfig.vcpu() != null && instanceConfig.vcpu().def() != null ? instanceConfig.vcpu().def() : 0;
        double loadPercent = vcpu > 0 ? response.cpuUsageCores() / vcpu * 100.0 : 0.0;
        int nodeCount = instanceConfig.nodesRequired(response.cpuUsageCores());
        return new KubernetesAPIAssetImpact(asset.name(), response, instanceConfig, loadPercent, nodeCount);
    }
}
