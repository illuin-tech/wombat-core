package tech.illuin.wombat.asset.model;

import tech.illuin.wombat.llm.LLMPrometheusProperties;
import tech.illuin.wombat.monitor.AssetType;
import tech.illuin.wombat.asset.model.profile.DynamicLLMProfile;

public record LLMPrometheusAsset(
    String environmentId,
    String environmentName,
    String name,
    DynamicLLMProfile profile,
    LLMPrometheusProperties properties
) implements Asset
{
    @Override
    public AssetType type()
    {
        return AssetType.LLM_PROMETHEUS;
    }
}
