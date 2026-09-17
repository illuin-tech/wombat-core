package tech.illuin.wombat.module.kubernetes_api;

import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetType;
import tech.illuin.wombat.core.context.WombatContext;
import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.core.source.WombatSource;
import tech.illuin.wombat.module.kubernetes_api.connector.KubernetesMultiClusterClientConfig;
import tech.illuin.wombat.module.kubernetes_api.source.KubernetesAPISource;

import java.util.Optional;

public class KubernetesAPIModule implements WombatModule
{
    @Override
    public AssetType type()
    {
        return AssetType.KUBERNETES_API;
    }

    @Override
    public Class<? extends Asset> assetClass()
    {
        return KubernetesAPIAsset.class;
    }

    @Override
    public Optional<WombatSource> createSource(WombatContext context)
    {
        return Optional.of(new KubernetesAPISource(KubernetesMultiClusterClientConfig.create(context)));
    }
}
