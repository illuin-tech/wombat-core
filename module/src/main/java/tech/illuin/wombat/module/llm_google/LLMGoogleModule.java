package tech.illuin.wombat.module.llm_google;

import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.context.WombatContext;
import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.core.source.WombatSource;
import tech.illuin.wombat.module.llm_google.connector.GoogleMultiClientConfig;
import tech.illuin.wombat.module.llm_google.source.LLMGoogleSource;

import java.util.Optional;

public class LLMGoogleModule implements WombatModule
{
    public static final AssetType TYPE = AssetType.of("tech.illuin", "wombat-module", "llm-google", ActivityRegime.MEASURED, ServiceFamily.LLM);

    @Override
    public AssetType type()
    {
        return TYPE;
    }

    @Override
    public Class<? extends Asset> assetClass()
    {
        return LLMGoogleAsset.class;
    }

    @Override
    public Optional<WombatSource> createSource(WombatContext context)
    {
        return Optional.of(new LLMGoogleSource(GoogleMultiClientConfig.create(context)));
    }
}
