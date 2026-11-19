package tech.illuin.wombat.module.llm_vertexai;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.secret.SecretAware;
import tech.illuin.wombat.core.secret.SecretKey;

import java.util.Set;

@JsonClassDescription("Google Cloud scope whose Vertex AI output tokens are read from Cloud Monitoring")
public record LLMVertexAIAsset(
    @JsonUnwrapped AssetIdentity identity,
    @NotBlank @JsonPropertyDescription("Base URL of the Cloud Monitoring API") String googleUrl,
    @NotBlank @Pattern(regexp = "(projects|folders|organizations)/[A-Za-z0-9_-]+")
    @JsonPropertyDescription("Scope of the query: projects/<id>, folders/<id> or organizations/<id>") String resourceName,
    @JsonPropertyDescription("Secret holding a service-account JSON key, Application Default Credentials when omitted") SecretKey credentialsKey,
    @JsonPropertyDescription("HTTP proxy URL, with a host and a port") String proxyUrl,
    @NotNull @JsonPropertyDescription("LLM profile and Cloud Monitoring query") LLMVertexAIProfile profile
) implements Asset, SecretAware
{
    public LLMVertexAIAsset(AssetIdentity identity, String googleUrl, String resourceName, SecretKey credentialsKey, LLMVertexAIProfile profile)
    {
        this(identity, googleUrl, resourceName, credentialsKey, null, profile);
    }

    @Override
    public AssetType type()
    {
        return LLMVertexAIModule.TYPE;
    }

    @Override
    public Set<String> requiredSecretKeys()
    {
        return this.usesServiceAccountKey() ? Set.of(this.credentialsKey.value()) : Set.of();
    }

    public boolean usesServiceAccountKey()
    {
        return this.credentialsKey != null;
    }
}
