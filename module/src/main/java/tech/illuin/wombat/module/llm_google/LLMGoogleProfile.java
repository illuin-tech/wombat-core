package tech.illuin.wombat.module.llm_google;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.LLMProfile;
import tech.illuin.wombat.core.asset.profile.LLMProvider;

import java.time.Duration;

public record LLMGoogleProfile(
    @NotNull @JsonProperty("provider") LLMProvider provider,
    @NotBlank @JsonProperty("model") String model,
    @NotNull @JsonProperty("location") Country location,
    @NotNull @Valid @JsonProperty("dynamic-profile") DynamicProfile dynamicProfile
) implements LLMProfile
{
    public record DynamicProfile(
        @NotBlank @JsonProperty("filter") String filter,
        @JsonProperty("window") Duration window
    ) {
        public Duration windowOrDefault()
        {
            return this.window == null ? Duration.ofMinutes(1) : this.window;
        }
    }
}
