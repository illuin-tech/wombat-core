package tech.illuin.wombat.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import tech.illuin.wombat.asset.model.profile.StaticLLMProfile;
import tech.illuin.wombat.monitor.AssetProperties;
import tech.illuin.wombat.monitor.AssetType;

@RegisterForReflection
public record LLMStaticProperties(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("profile") StaticLLMProfile profile
) implements AssetProperties
{
    @Override
    public AssetType type()
    {
        return AssetType.LLM_STATIC;
    }
}
