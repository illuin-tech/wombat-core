package tech.illuin.wombat.module.kubernetes_api;

import tech.illuin.wombat.core.asset.ActivityRegime;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetType;
import tech.illuin.wombat.core.asset.ServiceFamily;
import tech.illuin.wombat.core.context.WombatContext;
import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.core.source.WombatSource;
import tech.illuin.wombat.module.kubernetes_api.connector.KubernetesMultiClusterClientConfig;
import tech.illuin.wombat.module.kubernetes_api.source.KubernetesAPISource;

import java.util.Optional;

public class KubernetesAPIModule implements WombatModule
{
    public static final AssetType TYPE = AssetType.of("tech.illuin", "wombat-module", "kubernetes-api", ActivityRegime.MEASURED, ServiceFamily.KUBERNETES_CONTAINER);

    @Override
    public AssetType type()
    {
        return TYPE;
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
