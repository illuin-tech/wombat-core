package tech.illuin.wombat.module.llm_vertexai;

import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.module.asset.AssetProcessorFactory;
import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.module.llm_vertexai.activity.LLMVertexAIActivityResolver;
import tech.illuin.wombat.module.llm_vertexai.connector.GoogleClientConfig;

import java.util.Optional;

public class LLMVertexAIModule implements WombatModule
{
    public static final AssetType TYPE = AssetType.of("tech.illuin", "wombat-module", "llm-vertexai", ActivityRegime.MEASURED, ServiceFamily.LLM);

    @Override
    public AssetType type()
    {
        return TYPE;
    }

    @Override
    public Class<? extends Asset> assetClass()
    {
        return LLMVertexAIAsset.class;
    }

    @Override
    public Optional<AssetProcessorFactory<WombatActivityResolver>> activityResolver()
    {
        return Optional.of((services, asset) -> new LLMVertexAIActivityResolver(
            GoogleClientConfig.create((LLMVertexAIAsset) asset, services.secrets())
        ));
    }
}
