package tech.illuin.wombat.module.llm_anthropic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.secret.SecretAware;

import java.util.Set;

public record LLMAnthropicAsset(
    @JsonUnwrapped AssetIdentity identity,
    @JsonProperty("api-url") String apiUrl,
    @JsonProperty("proxy-url") String proxyUrl,
    @NotBlank @JsonProperty("admin-key") String adminKey,
    @NotNull @Valid @JsonProperty("profile") LLMAnthropicProfile profile
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
        if (this.adminKey == null || this.adminKey.isBlank())
            throw new IllegalStateException("Asset " + this.identity.id() + " declares no admin-key");

        return Set.of(this.adminKey);
    }

    public String resolvedApiUrl()
    {
        return this.apiUrl == null || this.apiUrl.isBlank() ? DEFAULT_API_URL : this.apiUrl;
    }
}
