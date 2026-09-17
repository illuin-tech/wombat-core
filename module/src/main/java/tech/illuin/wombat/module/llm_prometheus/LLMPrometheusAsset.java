package tech.illuin.wombat.module.llm_prometheus;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetType;
import tech.illuin.wombat.core.secret.SecretAware;
import tech.illuin.wombat.core.source.Monitorable;

import java.util.Set;


public record LLMPrometheusAsset(
    @JsonProperty("id") String id,
    @JsonProperty("environment-id") String environmentId,
    @JsonProperty("name") String name,
    @JsonProperty("prometheus-url") String prometheusUrl,
    @JsonProperty("proxy-url") String proxyUrl,
    @JsonProperty("username") String username,
    @JsonProperty("password-env") String passwordEnv,
    @JsonProperty("heartbeat-skip") int heartbeatSkip,
    @JsonProperty("profile") LLMPrometheusProfile profile
) implements Asset, SecretAware, Monitorable
{
    @Override
    public AssetType type()
    {
        return AssetType.LLM_PROMETHEUS;
    }

    @Override
    public Set<String> requiredSecretKeys()
    {
        if (!this.usesBasicAuth())
            return Set.of();
        if (this.passwordEnv == null || this.passwordEnv.isBlank())
            throw new IllegalStateException("Asset " + this.id + " declares a Prometheus username but no password-env");

        return Set.of(this.passwordEnv);
    }

    public boolean usesBasicAuth()
    {
        return this.username != null && !this.username.isBlank();
    }
}
