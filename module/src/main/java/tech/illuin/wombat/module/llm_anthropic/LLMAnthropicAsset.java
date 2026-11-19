package tech.illuin.wombat.module.llm_anthropic;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.secret.SecretAware;
import tech.illuin.wombat.core.secret.SecretKey;

import java.util.Set;

@JsonClassDescription("Anthropic organization whose usage is read from the Admin API usage report")
public record LLMAnthropicAsset(
    @JsonUnwrapped AssetIdentity identity,
    @JsonProperty(defaultValue = LLMAnthropicAsset.DEFAULT_API_URL) @JsonPropertyDescription("Base URL of the Anthropic API") String apiUrl,
    @JsonPropertyDescription("HTTP proxy URL, with a host and a port") String proxyUrl,
    @NotNull @JsonPropertyDescription("Secret holding an Anthropic Admin API key (sk-ant-admin...); a standard API key is rejected") SecretKey adminKey,
    @NotNull @JsonPropertyDescription("Usage scope and request estimation") LLMAnthropicProfile profile
) implements Asset, SecretAware
{
    public static final String DEFAULT_API_URL = "https://api.anthropic.com";

    @Override
    public AssetType type()
    {
        return LLMAnthropicModule.TYPE;
    }

    @Override
    public Set<String> requiredSecretKeys()
    {
        if (this.adminKey == null)
            throw new IllegalStateException("Asset " + this.identity.id() + " declares no adminKey");

        return Set.of(this.adminKey.value());
    }

    public String resolvedApiUrl()
    {
        return this.apiUrl == null || this.apiUrl.isBlank() ? DEFAULT_API_URL : this.apiUrl;
    }
}
