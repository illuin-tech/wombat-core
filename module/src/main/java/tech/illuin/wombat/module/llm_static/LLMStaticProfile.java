package tech.illuin.wombat.module.llm_static;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import tech.illuin.wombat.core.asset.profile.LLMProfile;
import tech.illuin.wombat.core.asset.profile.LLMProvider;

public record LLMStaticProfile(
    @NotNull @JsonProperty("provider") LLMProvider provider,
    @NotBlank @JsonProperty("model") String model,
    @NotBlank @JsonProperty("location") String location,
    @NotNull @Valid @JsonProperty("request-profile") RequestProfile requestProfile
) implements LLMProfile
{
    public record RequestProfile(
        @Positive @JsonProperty("output-token-count") int outputTokenCount,
        @Positive @JsonProperty("request-per-year") int requestPerYear
    ) {}
}
