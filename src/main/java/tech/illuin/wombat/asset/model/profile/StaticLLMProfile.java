package tech.illuin.wombat.asset.model.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.ecologits.model.EcologitsEstimationRequest;

public record StaticLLMProfile(
    @JsonProperty("provider") EcologitsEstimationRequest.Provider provider,
    @JsonProperty("model") String model,
    @JsonProperty("location") String location,
    @JsonProperty("request-profile") RequestProfile requestProfile
) implements LLMProfile
{

    public record RequestProfile(
        @JsonProperty("output-token-count") int outputTokenCount,
        @JsonProperty("request-per-year") int requestPerYear
    ) {}
}
