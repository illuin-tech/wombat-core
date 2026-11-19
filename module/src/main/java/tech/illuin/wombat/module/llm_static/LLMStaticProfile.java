package tech.illuin.wombat.module.llm_static;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.LLMProfile;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.module.llm_static.scenario.Scenario;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonClassDescription("Models and their modeled traffic")
public record LLMStaticProfile(
    @JsonProperty(defaultValue = "UTC") @JsonPropertyDescription("Time zone the scenarios' times are evaluated in, e.g. Europe/Paris") ZoneId zoneId,
    @NotNull @NotEmpty @JsonPropertyDescription("Models and their scenarios") List<@Valid ModelConfig> models
) implements LLMProfile
{
    public LLMStaticProfile
    {
        zoneId = zoneId != null ? zoneId : ZoneOffset.UTC;
        models = models != null ? List.copyOf(models) : List.of();
    }

    public LLMStaticProfile(List<ModelConfig> models)
    {
        this(ZoneOffset.UTC, models);
    }

    public LLMStaticProfile(LLMProvider provider, String model, Country location, List<Scenario> scenarios)
    {
        this(ZoneOffset.UTC, List.of(new ModelConfig(provider, model, location, scenarios)));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonClassDescription("Model and its superposed traffic scenarios")
    public record ModelConfig(
        @NotNull @JsonPropertyDescription("Foundation model provider") LLMProvider provider,
        @NotBlank @JsonPropertyDescription("Model identifier, as listed by Ecologits") String model,
        @NotNull @JsonPropertyDescription("Country where inference is assumed to occur") Country location,
        @NotNull @NotEmpty @JsonPropertyDescription("Superposed traffic scenarios") List<@Valid Scenario> scenarios
    ) {
        public ModelConfig
        {
            scenarios = scenarios != null ? List.copyOf(scenarios) : List.of();
        }
    }
}
