package tech.illuin.wombat.module.llm_vertexai;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import tech.illuin.wombat.core.asset.profile.LLMProfile;

import java.util.List;

public record LLMVertexAIProfile(
    @Positive @JsonProperty("outputTokensPerRequest") @JsonAlias("output-tokens-per-request") Integer outputTokensPerRequest,
    @JsonProperty("models") List<@NotBlank String> models
) implements LLMProfile
{
    public LLMVertexAIProfile
    {
        models = models == null ? List.of() : List.copyOf(models);
        outputTokensPerRequest = outputTokensPerRequest == null ? 500 : outputTokensPerRequest;
    }
}
