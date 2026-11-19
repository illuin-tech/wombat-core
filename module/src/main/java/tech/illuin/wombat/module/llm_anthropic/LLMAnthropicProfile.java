package tech.illuin.wombat.module.llm_anthropic;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.Positive;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.LLMProfile;

import java.util.List;

@JsonClassDescription("Scope of the usage report and request estimation")
public record LLMAnthropicProfile(
    @JsonProperty(defaultValue = "USA") @JsonPropertyDescription("Country where inference is assumed to occur") Country location,
    @Positive @JsonProperty(defaultValue = "500") @JsonPropertyDescription("Average output tokens per request, used to derive the request count") Integer outputTokensPerRequest,
    @JsonPropertyDescription("Model identifiers the report is restricted to, all models when empty") List<String> models,
    @JsonPropertyDescription("Workspace identifiers the report is restricted to, includes all workspaces when empty") List<String> workspaceIds,
    @JsonPropertyDescription("API key identifiers the report is restricted to, includes all API keys when empty") List<String> apiKeyIds
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
