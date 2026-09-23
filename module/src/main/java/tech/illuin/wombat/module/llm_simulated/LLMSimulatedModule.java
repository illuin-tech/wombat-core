package tech.illuin.wombat.module.llm_simulated;

import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetType;
import tech.illuin.wombat.core.module.WombatModule;

public class LLMSimulatedModule implements WombatModule
{
    @Override
    public AssetType type()
    {
        return AssetType.LLM_SIMULATED;
    }

    @Override
    public Class<? extends Asset> assetClass()
    {
        return LLMSimulatedAsset.class;
    }
}
