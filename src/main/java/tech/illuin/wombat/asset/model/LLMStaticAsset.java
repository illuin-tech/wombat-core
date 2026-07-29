package tech.illuin.wombat.asset.model;

import tech.illuin.wombat.llm.LLMStaticProperties;
import tech.illuin.wombat.monitor.AssetType;
import tech.illuin.wombat.asset.model.profile.StaticLLMProfile;

public record LLMStaticAsset(
    String environmentId,
    String environmentName,
    String name,
    StaticLLMProfile profile,
    LLMStaticProperties properties
) implements Asset
{
    @Override
    public AssetType type()
    {
        return AssetType.LLM_STATIC;
    }
}
