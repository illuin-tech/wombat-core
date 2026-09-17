package tech.illuin.wombat.module.llm_static;

import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.context.WombatContext;
import tech.illuin.wombat.module.llm_static.activity.LLMStaticActivityResolver;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetType;
import tech.illuin.wombat.core.module.WombatModule;

import java.util.Optional;

public class LLMStaticModule implements WombatModule
{
    @Override
    public AssetType type()
    {
        return AssetType.LLM_STATIC;
    }

    @Override
    public Class<? extends Asset> assetClass()
    {
        return LLMStaticAsset.class;
    }

    @Override
    public Optional<WombatActivityResolver> createActivityResolver(WombatContext context)
    {
        return Optional.of(new LLMStaticActivityResolver());
    }
}
