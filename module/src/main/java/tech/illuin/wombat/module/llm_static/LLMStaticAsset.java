package tech.illuin.wombat.module.llm_static;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.type.AssetType;

public record LLMStaticAsset(
    @JsonUnwrapped AssetIdentity identity,
    @NotNull @JsonProperty("profile") LLMStaticProfile profile
) implements Asset
{
    @Override
    public AssetType type()
    {
        return LLMStaticModule.TYPE;
    }
}
