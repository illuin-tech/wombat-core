package tech.illuin.wombat.module.llm_google;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.secret.SecretAware;

import java.util.Set;

public record LLMGoogleAsset(
    @JsonUnwrapped AssetIdentity identity,
    @NotBlank @JsonProperty("google-url") String googleUrl,
    @NotBlank @Pattern(regexp = "(projects|folders|organizations)/[A-Za-z0-9_-]+")
    @JsonProperty("resource-name") String resourceName,
    @JsonProperty("credentials-key") String credentialsKey,
    @JsonProperty("proxy-url") String proxyUrl,
    @NotNull @JsonProperty("profile") LLMGoogleProfile profile
) implements Asset, SecretAware
{
    public LLMGoogleAsset(AssetIdentity identity, String googleUrl, String resourceName, String credentialsKey, LLMGoogleProfile profile)
    {
        this(identity, googleUrl, resourceName, credentialsKey, null, profile);
    }

    @Override
    public AssetType type()
    {
        return LLMGoogleModule.TYPE;
    }

    @Override
    public Set<String> requiredSecretKeys()
    {
        return this.usesServiceAccountKey() ? Set.of(this.credentialsKey) : Set.of();
    }

    public boolean usesServiceAccountKey()
    {
        return this.credentialsKey != null && !this.credentialsKey.isBlank();
    }
}
