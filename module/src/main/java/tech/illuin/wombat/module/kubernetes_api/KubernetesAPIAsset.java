package tech.illuin.wombat.module.kubernetes_api;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.profile.AssetProfile;
import tech.illuin.wombat.core.asset.profile.BareServerProfile;
import tech.illuin.wombat.core.asset.profile.CloudServerProfile;
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
    @JsonPropertyDescription("Namespaces to poll, all namespaces when empty or omitted") Set<String> namespaces,
    @JsonPropertyDescription("Kubeconfig context to target, the active one when omitted") Optional<String> context,
    // The kebab-case @JsonAlias read assets persisted before 0.15, when properties were named that way
    @JsonPropertyDescription("Read timeout of the Kubernetes API requests (ISO-8601), the client's default when omitted") @JsonAlias("read-timeout") Optional<Duration> readTimeout,
    @JsonPropertyDescription("Polling interval, the monitor's default when omitted") Period period,
    @NotNull @Valid @JsonPropertyDescription("Server hardware profile, inferred from the cluster when omitted") KubernetesAPIServerProfile profile
) implements Asset, SecretAware, Monitorable
{
    public KubernetesAPIAsset
    {
        namespaces = namespaces != null ? Set.copyOf(namespaces) : Set.of();
        profile = profile != null ? profile : KubernetesAPIInferredProfile.DEFAULT;
    }

    @Override
    public AssetType type()
    {
        return KubernetesAPIModule.TYPE;
    }

    /**
     * Inferred profiles become the cloud or bare server profile they describe.
     */
    @Override
    public KubernetesAPIAsset withProfile(AssetProfile profile)
    {
        KubernetesAPIServerProfile serverProfile = switch (profile)
        {
            case KubernetesAPIServerProfile declared -> declared;
            case CloudServerProfile cloud -> new KubernetesAPICloudProfile(cloud.provider(), cloud.instanceType(), cloud.location(), cloud.lifespan());
            case BareServerProfile bare -> new KubernetesAPIBareServerProfile(bare.location(), bare.lifespan(), bare.cpu(), bare.ram(), bare.ssd(), bare.hdd(), bare.gpu());
            default -> throw new IllegalArgumentException("Asset-type " + this.type().name() + " cannot have a " + profile.getClass().getSimpleName() + " profile");
        };
        return new KubernetesAPIAsset(this.identity, this.kubeconfigKey, this.namespaces, this.context, this.readTimeout, this.period, serverProfile);
    }

    @Override
    public Set<String> requiredSecretKeys()
    {
        if (this.kubeconfigKey == null)
            throw new IllegalStateException("Asset " + this.identity.id() + " declares no kubeconfig-key");

        return Set.of(this.kubeconfigKey.value());
    }
}
