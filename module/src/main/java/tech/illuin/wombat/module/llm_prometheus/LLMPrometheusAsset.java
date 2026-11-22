package tech.illuin.wombat.module.llm_prometheus;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.secret.SecretAware;
import tech.illuin.wombat.core.secret.SecretKey;
import tech.illuin.wombat.core.source.Monitorable;
import tech.illuin.wombat.core.source.Period;

import java.util.Set;

@JsonClassDescription("LLM serving stack whose output token volume is polled from Prometheus")
public record LLMPrometheusAsset(
    @JsonUnwrapped AssetIdentity identity,
    // The kebab-case @JsonAlias read assets persisted before 0.15, when properties were named that way
    @NotBlank @JsonPropertyDescription("Base URL of the Prometheus server") @JsonAlias("prometheus-url") String prometheusUrl,
    @JsonPropertyDescription("HTTP proxy URL, with a host and a port") @JsonAlias("proxy-url") String proxyUrl,
    @JsonPropertyDescription("Basic authentication username") String username,
    @JsonPropertyDescription("Secret holding the basic authentication password, required with a username") @JsonAlias("password-key") SecretKey passwordKey,
    @JsonPropertyDescription("Polling interval, the monitor's default when omitted") Period period,
    @NotNull @Valid @JsonPropertyDescription("LLM profile and PromQL query") LLMPrometheusProfile profile
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
        if (this.passwordKey == null)
            throw new IllegalStateException("Asset " + this.identity.id() + " declares a Prometheus username but no password-key");

        return Set.of(this.passwordKey.value());
    }

    public boolean usesBasicAuth()
    {
        return this.username != null && !this.username.isBlank();
    }
}
