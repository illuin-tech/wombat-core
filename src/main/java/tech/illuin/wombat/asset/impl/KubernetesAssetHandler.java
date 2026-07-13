package tech.illuin.wombat.asset.impl;

import tech.illuin.wombat.asset.model.Asset;
import tech.illuin.wombat.asset.model.KubernetesAsset;
import tech.illuin.wombat.asset.AssetHandler;
import tech.illuin.wombat.boavizta.BoaviztaClient;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceConfigResponse;
import tech.illuin.wombat.handler.KubernetesImpactService;
import tech.illuin.wombat.handler.model.KubernetesImpactResponse;
import tech.illuin.wombat.monitor.AssetProperties;
import tech.illuin.wombat.monitor.AssetType;
import tech.illuin.wombat.persistence.NoCPUUsageException;
import tech.illuin.wombat.persistence.model.TimeRange;
import tech.illuin.wombat.profile.model.InfrastructureProfile;
import tech.illuin.wombat.ui.KubernetesAssetImpact;

import java.util.Collection;
import java.util.List;

public class KubernetesAssetHandler implements AssetHandler<KubernetesAssetImpact>
{
    private final KubernetesImpactService kubernetesImpactService;
    private final BoaviztaClient boaviztaClient;

    public KubernetesAssetHandler(KubernetesImpactService kubernetesImpactService, BoaviztaClient boaviztaClient)
    {
        this.kubernetesImpactService = kubernetesImpactService;
        this.boaviztaClient = boaviztaClient;
    }

    @Override
    public boolean accept(AssetProperties assetProperties)
    {
        return assetProperties.type().equals(AssetType.KUBERNETES_API);
    }

    @Override
    public KubernetesAssetImpact computeImpact(Asset asset, Collection<String> assetServices, TimeRange timeRange) throws NoCPUUsageException
    {
        KubernetesAsset kubernetesAsset = (KubernetesAsset) asset;
        KubernetesImpactResponse response = this.kubernetesImpactService.computeImpactResponse(timeRange, assetServices, List.of(kubernetesAsset)).getFirst();
        InfrastructureProfile profile = kubernetesAsset.profile();
        BoaviztaInstanceConfigResponse instanceConfig = this.boaviztaClient.getInstanceConfig(profile.provider(), profile.instanceType());
        int vcpu = instanceConfig.vcpu() != null && instanceConfig.vcpu().def() != null ? instanceConfig.vcpu().def() : 0;
        double loadPercent = vcpu > 0 ? response.cpuUsageCores() / vcpu * 100.0 : 0.0;
        int nodeCount = instanceConfig.nodesRequired(response.cpuUsageCores());
        return new KubernetesAssetImpact(asset.name(), profile.measureType(), response, instanceConfig, loadPercent, nodeCount);
    }
}
