package tech.illuin.wombat.module.llm_static;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetType;

public record LLMStaticAsset(
    @NotBlank @JsonProperty("id") String id,
    @NotBlank @JsonProperty("environment-id") String environmentId,
    @NotBlank @JsonProperty("name") String name,
    @NotNull @JsonProperty("profile") LLMStaticProfile profile
) implements Asset
{
    @Override
    public AssetType type()
    {
        return LLMStaticModule.TYPE;
    }
}
