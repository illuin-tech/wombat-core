package tech.illuin.wombat.module.llm_prometheus;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.asset.profile.LLMProfile;
import tech.illuin.wombat.core.asset.profile.LLMProvider;

public record LLMPrometheusProfile(
    @NotNull @JsonProperty("provider") LLMProvider provider,
    @NotBlank @JsonProperty("model") String model,
    @NotBlank @JsonProperty("location") String location,
    @NotNull @Valid @JsonProperty("dynamic-profile") DynamicProfile dynamicProfile
) implements LLMProfile
{
    public record DynamicProfile(
        @NotBlank @JsonProperty("query") String query
    ) {}
}
