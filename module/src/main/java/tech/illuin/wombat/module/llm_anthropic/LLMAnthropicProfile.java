package tech.illuin.wombat.module.llm_anthropic;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.LLMProfile;

import java.util.List;

public record LLMAnthropicProfile(
    @NotNull @JsonProperty("location") Country location,
    @Positive @JsonProperty("output-tokens-per-request") int outputTokensPerRequest,
    @JsonProperty("models") List<String> models,
    @JsonProperty("workspace-ids") List<String> workspaceIds,
    @JsonProperty("api-key-ids") List<String> apiKeyIds
) implements LLMProfile
{
    public LLMAnthropicProfile
    {
        models = models == null ? List.of() : List.copyOf(models);
        workspaceIds = workspaceIds == null ? List.of() : List.copyOf(workspaceIds);
        apiKeyIds = apiKeyIds == null ? List.of() : List.copyOf(apiKeyIds);
    }
}
