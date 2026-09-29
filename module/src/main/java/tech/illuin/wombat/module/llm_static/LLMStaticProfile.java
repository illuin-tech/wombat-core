package tech.illuin.wombat.module.llm_static;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import tech.illuin.wombat.core.asset.profile.LLMProfile;
import tech.illuin.wombat.core.asset.profile.LLMProvider;

import java.util.List;

public record LLMStaticProfile(
    @NotNull @NotEmpty @JsonProperty("models") List<@Valid ModelConfig> models
) implements LLMProfile
{
    public LLMStaticProfile(LLMProvider provider, String model, String location, RequestProfile requestProfile)
    {
        this(List.of(new ModelConfig(provider, model, location, requestProfile)));
    }

    public record ModelConfig(
        @NotNull @JsonProperty("provider") LLMProvider provider,
        @NotBlank @JsonProperty("model") String model,
        @NotBlank @JsonProperty("location") String location,
        @NotNull @Valid @JsonProperty("request-profile") RequestProfile requestProfile
    ) {}

    public record RequestProfile(
        @PositiveOrZero @JsonProperty("output-token-count") int outputTokenCount,
        @PositiveOrZero @JsonProperty("request-per-year") int requestPerYear
    ) {}
}
