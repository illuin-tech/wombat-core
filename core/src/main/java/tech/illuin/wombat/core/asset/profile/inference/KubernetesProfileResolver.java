package tech.illuin.wombat.core.asset.profile.inference;

import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.profile.AssetProfile;
import tech.illuin.wombat.core.asset.profile.ServerProfile;
import tech.illuin.wombat.core.evaluation.WombatProfileResolver;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.function.Function;

/**
 * Infers the server profile of a Kubernetes asset declaring an {@link InferableServerProfile}, and keeps it for
 * {@code ttl} so that evaluations do not each query the cluster. A failed inference is not kept, and is attempted
 * again on the next evaluation.
 */
public final class KubernetesProfileResolver implements WombatProfileResolver, AutoCloseable
{
    public static final Duration DEFAULT_TTL = Duration.ofHours(1);

    private final ClusterInventory inventory;
    private final Function<Asset, Set<String>> namespaces;
    private final KubernetesProfileInference inference;
    private final Duration ttl;
    private final Clock clock;
    private Inferred inferred;

    /**
     * @param namespaces the namespaces an asset polls, all of them when empty
     */
    public KubernetesProfileResolver(ClusterInventory inventory, Function<Asset, Set<String>> namespaces)
    {
        this(inventory, namespaces, new KubernetesProfileInference(), DEFAULT_TTL, Clock.systemUTC());
    }

    public KubernetesProfileResolver(ClusterInventory inventory, Function<Asset, Set<String>> namespaces, KubernetesProfileInference inference, Duration ttl, Clock clock)
    {
        this.inventory = inventory;
        this.namespaces = namespaces;
        this.inference = inference;
        this.ttl = ttl;
        this.clock = clock;
    }

    @Override
    public boolean accept(Asset asset)
    {
        return asset.profile() instanceof InferableServerProfile;
    }

    @Override
    public synchronized AssetProfile resolve(Asset asset) throws ProfileInferenceException
    {
        InferableServerProfile hints = (InferableServerProfile) asset.profile();
        Set<String> polled = this.namespaces.apply(asset);
        Instant now = this.clock.instant();

        if (this.inferred != null && this.inferred.matches(hints, polled) && now.isBefore(this.inferred.expiry()))
            return this.inferred.profile();

        ServerProfile profile = this.inference.infer(this.inventory, polled, hints);
        this.inferred = new Inferred(hints, polled, profile, now.plus(this.ttl));
        return profile;
    }

    @Override
    public void close()
    {
        this.inventory.close();
    }

    /* Keyed by the hints and namespaces the inference was made for, which an asset update may change. */
    private record Inferred(
        InferableServerProfile hints,
        Set<String> namespaces,
        ServerProfile profile,
        Instant expiry
    ) {
        boolean matches(InferableServerProfile hints, Set<String> namespaces)
        {
            return this.hints.equals(hints) && this.namespaces.equals(namespaces);
        }
    }
}
