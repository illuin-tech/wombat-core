package tech.illuin.wombat.module.llm_vertexai;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import tech.illuin.wombat.core.asset.profile.LLMProfile;

import java.util.List;

@JsonClassDescription("Models the query is restricted to and request estimation")
public record LLMVertexAIProfile(
    @Positive @JsonProperty(defaultValue = "500") @JsonPropertyDescription("Average output tokens per request, used to derive the request count") Integer outputTokensPerRequest,
    @JsonPropertyDescription("Model identifiers the query is restricted to, includes all models when empty") List<@NotBlank String> models
) implements LLMProfile
{
    public LLMVertexAIProfile
    {
        models = models == null ? List.of() : List.copyOf(models);
        outputTokensPerRequest = outputTokensPerRequest == null ? 500 : outputTokensPerRequest;
    }
}
