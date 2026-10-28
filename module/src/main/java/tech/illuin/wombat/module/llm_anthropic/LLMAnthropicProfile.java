package tech.illuin.wombat.module.llm_anthropic;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Positive;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.LLMProfile;

import java.util.List;

public record LLMAnthropicProfile(
    @JsonProperty("location") Country location,
    @Positive @JsonProperty("outputTokensPerRequest") @JsonAlias("output-tokens-per-request") Integer outputTokensPerRequest,
    @JsonProperty("models") List<String> models,
    @JsonProperty("workspaceIds") @JsonAlias("workspace-ids") List<String> workspaceIds,
    @JsonProperty("apiKeyIds") @JsonAlias("api-key-ids") List<String> apiKeyIds
) implements LLMProfile
{
    public LLMAnthropicProfile
    {
        location = location == null ? Country.US : location;
        outputTokensPerRequest = outputTokensPerRequest == null ? 500 : outputTokensPerRequest;
        models = models == null ? List.of() : List.copyOf(models);
        workspaceIds = workspaceIds == null ? List.of() : List.copyOf(workspaceIds);
        apiKeyIds = apiKeyIds == null ? List.of() : List.copyOf(apiKeyIds);
    }
}
