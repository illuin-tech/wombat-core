package tech.illuin.wombat.module.llm_static;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public record LLMStaticProfile(
    @JsonProperty("zoneId") @JsonAlias("zone-id") ZoneId zoneId,
    @NotNull @NotEmpty @JsonProperty("models") List<@Valid ModelConfig> models
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
    public record ModelConfig(
        @NotNull @JsonProperty("provider") LLMProvider provider,
        @NotBlank @JsonProperty("model") String model,
        @NotNull @JsonProperty("location") Country location,
        @NotNull @NotEmpty @JsonProperty("scenarios") List<@Valid Scenario> scenarios
    ) {
        public ModelConfig
        {
            scenarios = scenarios != null ? List.copyOf(scenarios) : List.of();
        }
    }
}
