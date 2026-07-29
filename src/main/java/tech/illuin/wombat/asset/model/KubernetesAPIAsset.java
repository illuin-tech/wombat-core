package tech.illuin.wombat.asset.model;

import tech.illuin.wombat.kubernetes.KubernetesAPIAssetProperties;
import tech.illuin.wombat.monitor.AssetType;
import tech.illuin.wombat.asset.model.profile.InfrastructureProfile;

public record KubernetesAPIAsset(
    String environmentId,
    String environmentName,
    String name,
    InfrastructureProfile profile,
    KubernetesAPIAssetProperties properties
) implements Asset
{
    @Override
    public AssetType type()
    {
        return AssetType.KUBERNETES_API;
    }
}
