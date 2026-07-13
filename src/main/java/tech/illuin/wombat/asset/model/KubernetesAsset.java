package tech.illuin.wombat.asset.model;

import tech.illuin.wombat.kubernetes.KubernetesAssetProperties;
import tech.illuin.wombat.monitor.AssetType;
import tech.illuin.wombat.profile.model.InfrastructureProfile;

public record KubernetesAsset(
    String environmentId,
    String environmentName,
    String name,
    InfrastructureProfile profile,
    KubernetesAssetProperties properties
) implements Asset
{
    @Override
    public AssetType type()
    {
        return AssetType.KUBERNETES_API;
    }
}
