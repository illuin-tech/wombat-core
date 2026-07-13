package tech.illuin.wombat.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import tech.illuin.wombat.monitor.AssetProperties;
import tech.illuin.wombat.monitor.AssetType;


@RegisterForReflection
public record LLMProperties(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("profile-id") String profileId
) implements AssetProperties
{
    @Override
    public AssetType type()
    {
        return AssetType.LLM_STATIC;
    }
}
