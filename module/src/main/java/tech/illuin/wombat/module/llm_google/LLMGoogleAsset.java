package tech.illuin.wombat.module.llm_google;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.secret.SecretAware;
import tech.illuin.wombat.core.source.Monitorable;

import java.util.Set;

public record LLMGoogleAsset(
    @JsonUnwrapped AssetIdentity identity,
    @NotBlank @JsonProperty("google-url") String googleUrl,
    @NotBlank @JsonProperty("project-id") String projectId,
    @JsonProperty("credentials-key") String credentialsKey,
    @JsonProperty("heartbeat-skip") int heartbeatSkip,
    @NotNull @JsonProperty("profile") LLMGoogleProfile profile
) implements Asset, SecretAware, Monitorable
{
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
