package tech.illuin.wombat.module.llm_prometheus;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.core.asset.profile.LLMProfile;
import tech.illuin.wombat.core.asset.profile.LLMProvider;

public record LLMPrometheusProfile(
    @JsonProperty("provider") LLMProvider provider,
    @JsonProperty("model") String model,
    @JsonProperty("location") String location,
    @JsonProperty("dynamic-profile") DynamicProfile dynamicProfile
) implements LLMProfile
{
    public record DynamicProfile(
        @JsonProperty("query") String query
    ) {}
}
