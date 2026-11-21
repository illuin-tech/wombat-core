package tech.illuin.wombat.module.kubernetes_api;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.secret.SecretAware;
import tech.illuin.wombat.core.secret.SecretKey;
import tech.illuin.wombat.core.source.Monitorable;
import tech.illuin.wombat.core.source.Period;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@JsonClassDescription("Kubernetes cluster whose container CPU usage is polled from its API")
// Assets persisted with a kubeconfig path still load, but the path is never read: they need a kubeconfigKey
@JsonIgnoreProperties({"configPath", "config-path"})
public record KubernetesAPIAsset(
    @JsonUnwrapped AssetIdentity identity,
    @NotNull @JsonPropertyDescription("Secret holding the kubeconfig contents") SecretKey kubeconfigKey,
    @JsonPropertyDescription("Namespace to poll, all namespaces when omitted") String namespace,
    @JsonPropertyDescription("Kubeconfig context to target, the active one when omitted") Optional<String> context,
    // The kebab-case @JsonAlias read assets persisted before 0.15, when properties were named that way
    @JsonPropertyDescription("Read timeout of the Kubernetes API requests (ISO-8601), the client's default when omitted") @JsonAlias("read-timeout") Optional<Duration> readTimeout,
    @JsonPropertyDescription("Polling interval, the monitor's default when omitted") Period period,
    @NotNull @JsonPropertyDescription("Server hardware profile") KubernetesAPIServerProfile profile
) implements Asset, SecretAware, Monitorable
{
    @Override
    public AssetType type()
    {
        return KubernetesAPIModule.TYPE;
    }

    @Override
    public Set<String> requiredSecretKeys()
    {
        if (this.kubeconfigKey == null)
            throw new IllegalStateException("Asset " + this.identity.id() + " declares no kubeconfig-key");

        return Set.of(this.kubeconfigKey.value());
    }
}
