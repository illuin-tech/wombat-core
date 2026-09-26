package tech.illuin.wombat.module.llm_simulated;

import tech.illuin.wombat.core.asset.ActivityRegime;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetType;
import tech.illuin.wombat.core.asset.ServiceFamily;
import tech.illuin.wombat.core.module.WombatModule;

public class LLMSimulatedModule implements WombatModule
{
    public static final AssetType TYPE = AssetType.of("tech.illuin", "wombat-module", "llm-simulated", ActivityRegime.MODELED, ServiceFamily.LLM);

    @Override
    public AssetType type()
    {
        return TYPE;
    }

    @Override
    public Class<? extends Asset> assetClass()
    {
        return LLMSimulatedAsset.class;
    }
}
