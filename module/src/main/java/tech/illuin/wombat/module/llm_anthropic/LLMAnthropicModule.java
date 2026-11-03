package tech.illuin.wombat.module.llm_anthropic;

import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.context.WombatContext;
import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.module.llm_anthropic.activity.LLMAnthropicActivityResolver;
import tech.illuin.wombat.module.llm_anthropic.connector.AnthropicMultiClientConfig;

import java.util.Optional;

public class LLMAnthropicModule implements WombatModule
{
    public static final AssetType TYPE = AssetType.of("tech.illuin", "wombat-module", "llm-anthropic", ActivityRegime.MEASURED, ServiceFamily.LLM);

    @Override
    public AssetType type()
    {
        return TYPE;
    }

    @Override
    public Class<? extends Asset> assetClass()
    {
        return LLMAnthropicAsset.class;
    }

    @Override
    public Optional<WombatActivityResolver> createActivityResolver(WombatContext context)
    {
        return Optional.of(new LLMAnthropicActivityResolver(AnthropicMultiClientConfig.create(context)));
    }
}
