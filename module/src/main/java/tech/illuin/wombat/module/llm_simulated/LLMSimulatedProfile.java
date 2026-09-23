package tech.illuin.wombat.module.llm_simulated;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.core.asset.profile.LLMProfile;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.module.llm_static.LLMStaticProfile;

public record LLMSimulatedProfile(
    @JsonProperty("provider") LLMProvider provider,
    @JsonProperty("model") String model,
    @JsonProperty("location") String location,
    @JsonProperty("request-profile") LLMStaticProfile.RequestProfile requestProfile
) implements LLMProfile {}
