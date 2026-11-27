package tech.illuin.wombat.module.kubernetes_api;

import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.module.kubernetes_api.profile.KubernetesProfileResolver;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.evaluation.WombatProfileResolver;
import tech.illuin.wombat.core.module.ProcessorFactory;
import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.core.source.WombatSource;
import tech.illuin.wombat.module.kubernetes_api.connector.KubernetesClientConfig;
import tech.illuin.wombat.module.kubernetes_api.connector.KubernetesClusterInventory;
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
    public Optional<Class<? extends Asset>> assetClass()
    {
        return Optional.of(KubernetesAPIAsset.class);
    }

    @Override
    public Optional<ProcessorFactory<WombatSource>> source()
    {
        return Optional.of((services, asset) -> new KubernetesAPISource(
            KubernetesClientConfig.create((KubernetesAPIAsset) asset, services.secrets())
        ));
    }

    /**
     * Infers the profile of assets declaring an inferred one, through the kubeconfig they are polled with.
     */
    @Override
    public Optional<ProcessorFactory<WombatProfileResolver>> profileResolver()
    {
        return Optional.of((services, asset) -> new KubernetesProfileResolver(
            new KubernetesClusterInventory(() -> KubernetesClientConfig.create((KubernetesAPIAsset) asset, services.secrets())),
            inferred -> ((KubernetesAPIAsset) inferred).namespaces()
        ));
    }
}
