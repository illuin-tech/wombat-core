package tech.illuin.wombat.module.llm_static;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetType;

public record LLMStaticAsset(
    @JsonProperty("id") String id,
    @JsonProperty("environment-id") String environmentId,
    @JsonProperty("name") String name,
    @JsonProperty("profile") LLMStaticProfile profile
) implements Asset
{
    @Override
    public AssetType type()
    {
        return AssetType.LLM_STATIC;
    }
}
