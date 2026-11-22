package tech.illuin.wombat.module.llm_static;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.type.AssetType;

@JsonClassDescription("LLM usage modeled from static scenarios")
public record LLMStaticAsset(
    @JsonUnwrapped AssetIdentity identity,
    @NotNull @Valid @JsonPropertyDescription("Models and their traffic scenarios") LLMStaticProfile profile
) implements Asset
{
    @Override
    public AssetType type()
    {
        return LLMStaticModule.TYPE;
    }
}
