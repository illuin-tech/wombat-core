package tech.illuin.wombat.module.kubernetes_api;

import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.module.ProcessorFactory;
import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.core.source.WombatSource;
import tech.illuin.wombat.module.kubernetes_api.connector.KubernetesClientConfig;
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
    public Optional<ProcessorFactory<WombatSource>> source()
    {
        return Optional.of((services, asset) -> new KubernetesAPISource(
            KubernetesClientConfig.create((KubernetesAPIAsset) asset)
        ));
    }
}
