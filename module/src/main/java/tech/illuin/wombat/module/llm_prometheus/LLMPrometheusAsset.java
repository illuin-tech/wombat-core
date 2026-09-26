package tech.illuin.wombat.module.llm_prometheus;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetType;
import tech.illuin.wombat.core.secret.SecretAware;
import tech.illuin.wombat.core.source.Monitorable;

import java.util.Set;

public record LLMPrometheusAsset(
    @NotBlank @JsonProperty("id") String id,
    @NotBlank @JsonProperty("environment-id") String environmentId,
    @NotBlank @JsonProperty("name") String name,
    @NotBlank @JsonProperty("prometheus-url") String prometheusUrl,
    @JsonProperty("proxy-url") String proxyUrl,
    @JsonProperty("username") String username,
    @JsonProperty("password-key") String passwordKey,
    @JsonProperty("heartbeat-skip") int heartbeatSkip,
    @NotNull @JsonProperty("profile") LLMPrometheusProfile profile
) implements Asset, SecretAware, Monitorable
{
    @Override
    public AssetType type()
    {
        return LLMPrometheusModule.TYPE;
    }

    @Override
    public Set<String> requiredSecretKeys()
    {
        if (!this.usesBasicAuth())
            return Set.of();
        if (this.passwordKey == null || this.passwordKey.isBlank())
            throw new IllegalStateException("Asset " + this.id + " declares a Prometheus username but no password-key");

        return Set.of(this.passwordKey);
    }

    public boolean usesBasicAuth()
    {
        return this.username != null && !this.username.isBlank();
    }
}
