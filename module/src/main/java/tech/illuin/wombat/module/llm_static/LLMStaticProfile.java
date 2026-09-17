package tech.illuin.wombat.module.llm_static;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.core.asset.profile.LLMProfile;
import tech.illuin.wombat.core.asset.profile.LLMProvider;

public record LLMStaticProfile(
    @JsonProperty("provider") LLMProvider provider,
    @JsonProperty("model") String model,
    @JsonProperty("location") String location,
    @JsonProperty("request-profile") RequestProfile requestProfile
) implements LLMProfile
{
    public record RequestProfile(
        @JsonProperty("output-token-count") int outputTokenCount,
        @JsonProperty("request-per-year") int requestPerYear
    ) {}
}
