package tech.illuin.wombat.module.kubernetes_simulated;

import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetType;
import tech.illuin.wombat.core.module.WombatModule;

public class KubernetesSimulatedModule implements WombatModule
{
    @Override
    public AssetType type()
    {
        return AssetType.KUBERNETES_SIMULATED;
    }

    @Override
    public Class<? extends Asset> assetClass()
    {
        return KubernetesSimulatedAsset.class;
    }
}
