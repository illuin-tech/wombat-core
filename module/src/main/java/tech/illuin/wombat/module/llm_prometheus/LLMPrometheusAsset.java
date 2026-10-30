package tech.illuin.wombat.module.llm_prometheus;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.secret.SecretAware;
import tech.illuin.wombat.core.source.Monitorable;
import tech.illuin.wombat.core.source.Period;

import java.util.Set;

public record LLMPrometheusAsset(
    @JsonUnwrapped AssetIdentity identity,
    @NotBlank @JsonProperty("prometheusUrl") @JsonAlias("prometheus-url") String prometheusUrl,
    @JsonProperty("proxyUrl") @JsonAlias("proxy-url") String proxyUrl,
    @JsonProperty("username") String username,
    @JsonProperty("passwordKey") @JsonAlias("password-key") String passwordKey,
    @JsonProperty("period") Period period,
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
            throw new IllegalStateException("Asset " + this.identity.id() + " declares a Prometheus username but no password-key");

        return Set.of(this.passwordKey);
    }

    public boolean usesBasicAuth()
    {
        return this.username != null && !this.username.isBlank();
    }
}
