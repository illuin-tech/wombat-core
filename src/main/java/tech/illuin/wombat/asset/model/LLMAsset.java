package tech.illuin.wombat.asset.model;

import tech.illuin.wombat.llm.LLMProperties;
import tech.illuin.wombat.monitor.AssetType;
import tech.illuin.wombat.profile.model.LLMProfile;


public record LLMAsset(
    String environmentId,
    String environmentName,
    String name,
    LLMProfile profile,
    LLMProperties properties
) implements Asset
{
    @Override
    public AssetType type()
    {
        return AssetType.LLM_STATIC;
    }
}
