package tech.illuin.wombat.profile.persistence;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ProfileData.InfrastructureData.class, name = "INFRASTRUCTURE"),
    @JsonSubTypes.Type(value = ProfileData.LLMData.class, name = "LLM")
})
public sealed interface ProfileData
{
    record InfrastructureData(
        @JsonProperty("instance_type") String instanceType,
        @JsonProperty("lifespan") int lifespan
    ) implements ProfileData {}

    record LLMData(
        @JsonProperty("model") String model,
        @JsonProperty("output_token_count") int outputTokenCount,
        @JsonProperty("request_per_year") int requestPerYear
    ) implements ProfileData {}
}
