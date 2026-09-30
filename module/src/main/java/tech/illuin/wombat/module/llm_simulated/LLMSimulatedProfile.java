package tech.illuin.wombat.module.llm_simulated;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.asset.profile.LLMProfile;
import tech.illuin.wombat.core.asset.profile.LLMProvider;

public record LLMSimulatedProfile(
    @NotNull @JsonProperty("provider") LLMProvider provider,
    @NotBlank @JsonProperty("model") String model,
    @NotBlank @JsonProperty("location") String location
) implements LLMProfile
{}
