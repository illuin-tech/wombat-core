package tech.illuin.wombat.asset.model.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.ecologits.model.EcologitsEstimationRequest;

public record DynamicLLMProfile(
    @JsonProperty("provider") EcologitsEstimationRequest.Provider provider,
    @JsonProperty("model") String model,
    @JsonProperty("location") String location,
    @JsonProperty("dynamic-profile") DynamicProfile dynamicProfile
) implements LLMProfile
{

    public record DynamicProfile(
        @JsonProperty("query") String query
    ) {}
}
