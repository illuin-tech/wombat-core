package tech.illuin.wombat.ecologits.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EcologitsEstimationRequest(
    @JsonProperty("provider") Provider provider,
    @JsonProperty("model_name") String modelName,
    @JsonProperty("output_token_count") int outputTokenCount,
    @JsonProperty("electricity_mix_zone") String electricityMixZone
) {
    public enum Provider
    {
        anthropic, mistralai, openai, huggingface_hub, cohere, google_genai
    }
}
