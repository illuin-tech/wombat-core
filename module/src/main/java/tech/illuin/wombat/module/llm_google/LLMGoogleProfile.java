package tech.illuin.wombat.module.llm_google;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.LLMProfile;
import tech.illuin.wombat.core.asset.profile.LLMProvider;

import java.util.List;

public record LLMGoogleProfile(
    @NotNull @JsonProperty("provider") LLMProvider provider,
    @NotNull @JsonProperty("location") Country location,
    @Positive @JsonProperty("output-tokens-per-request") int outputTokensPerRequest,
    @JsonProperty("models") List<@NotBlank String> models
) implements LLMProfile
{
    public LLMGoogleProfile
    {
        models = models == null ? List.of() : List.copyOf(models);
    }
}
