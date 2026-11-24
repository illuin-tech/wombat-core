package tech.illuin.wombat.module.llm_anthropic;

import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.module.ProcessorFactory;
import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.module.llm_anthropic.activity.LLMAnthropicActivityResolver;
import tech.illuin.wombat.module.llm_anthropic.connector.AnthropicClientConfig;

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
    public Optional<Class<? extends Asset>> assetClass()
    {
        return Optional.of(LLMAnthropicAsset.class);
    }

    @Override
    public Optional<ProcessorFactory<WombatActivityResolver>> activityResolver()
    {
        return Optional.of((services, asset) -> new LLMAnthropicActivityResolver(
            AnthropicClientConfig.create((LLMAnthropicAsset) asset, services.secrets())
        ));
    }
}
