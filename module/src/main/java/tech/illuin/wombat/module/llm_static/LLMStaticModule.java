package tech.illuin.wombat.module.llm_static;

import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.asset.ActivityRegime;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetType;
import tech.illuin.wombat.core.asset.ServiceFamily;
import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.module.llm_static.activity.LLMStaticActivityResolver;

import java.util.Optional;

public class LLMStaticModule implements WombatModule
{
    public static final AssetType TYPE = AssetType.of("tech.illuin", "wombat-module", "llm-static", ActivityRegime.MODELED, ServiceFamily.LLM);

    @Override
    public AssetType type()
    {
        return TYPE;
    }

    @Override
    public Class<? extends Asset> assetClass()
    {
        return LLMStaticAsset.class;
    }

    @Override
    public Optional<WombatActivityResolver> createActivityResolver()
    {
        return Optional.of(new LLMStaticActivityResolver());
    }
}
