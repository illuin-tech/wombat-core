package tech.illuin.wombat.module.kubernetes_api;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.source.Monitorable;
import tech.illuin.wombat.core.source.Period;

import java.time.Duration;
import java.util.Optional;

@JsonClassDescription("Kubernetes cluster whose container CPU usage is polled from its API")
public record KubernetesAPIAsset(
    @JsonUnwrapped AssetIdentity identity,
    // The kebab-case @JsonAlias read assets persisted before 0.15, when properties were named that way
    @NotBlank @JsonPropertyDescription("Path to the kubeconfig file") @JsonAlias("config-path") String configPath,
    @JsonPropertyDescription("Namespace to poll, all namespaces when omitted") String namespace,
    @JsonPropertyDescription("Kubeconfig context to target, the active one when omitted") Optional<String> context,
    @JsonPropertyDescription("Read timeout of the Kubernetes API requests (ISO-8601), the client's default when omitted") @JsonAlias("read-timeout") Optional<Duration> readTimeout,
    @JsonPropertyDescription("Polling interval, the monitor's default when omitted") Period period,
    @NotNull @JsonPropertyDescription("Server hardware profile") KubernetesAPIServerProfile profile
) implements Asset, Monitorable
{
    @Override
    public AssetType type()
    {
        return KubernetesAPIModule.TYPE;
    }
}
