package tech.illuin.wombat.module.kubernetes_static;

import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.context.WombatContext;
import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.module.kubernetes_static.activity.KubernetesStaticActivityResolver;

import java.util.Optional;

public class KubernetesStaticModule implements WombatModule
{
    public static final AssetType TYPE = AssetType.of("tech.illuin", "wombat-module", "kubernetes-static", ActivityRegime.MODELED, ServiceFamily.KUBERNETES_CONTAINER);

    @Override
    public AssetType type()
    {
        return TYPE;
    }

    @Override
    public Class<? extends Asset> assetClass()
    {
        return KubernetesStaticAsset.class;
    }

    @Override
    public Optional<WombatActivityResolver> createActivityResolver(WombatContext context)
    {
        return Optional.of(new KubernetesStaticActivityResolver());
    }
}
