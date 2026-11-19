package tech.illuin.wombat.module.llm_prometheus;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.LLMProfile;
import tech.illuin.wombat.core.asset.profile.LLMProvider;

@JsonClassDescription("Served model and the PromQL query measuring its output tokens")
public record LLMPrometheusProfile(
    @NotNull @JsonPropertyDescription("Model provider") LLMProvider provider,
    @NotBlank @JsonPropertyDescription("Model identifier") String model,
    @NotNull @JsonPropertyDescription("Country where inference occurs") Country location,
    // The kebab-case @JsonAlias read assets persisted before 0.15, when properties were named that way
    @NotNull @Valid @JsonPropertyDescription("PromQL query configuration") @JsonAlias("dynamic-profile") DynamicProfile dynamicProfile
) implements LLMProfile
{
    public record DynamicProfile(
        @NotBlank @JsonPropertyDescription("PromQL query returning the output token volume over the sampling window") String query
    ) {}
}
