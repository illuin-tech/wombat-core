package tech.illuin.wombat.core.activity.commons;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public record AssetFilter(
    Set<Environment> environments
) {
    public AssetFilter
    {
        environments = environments == null ? Set.of() : Set.copyOf(environments);
    }

    public AssetFilter(Environment environment)
    {
        this(environment == null ? Set.of() : Set.of(environment));
    }

    public boolean accepts(String environmentId, String assetId)
    {
        if (this.environments.isEmpty())
            return true;

        return this.environment(environmentId)
            .map(environment -> environment.assets().isEmpty() || environment.assetIds().contains(assetId))
            .orElse(false);
    }

    public Optional<Environment> environment(String environmentId)
    {
        return this.environments.stream()
            .filter(environment -> environment.id().equals(environmentId))
            .findFirst();
    }

    public Set<String> environmentIds()
    {
        return this.environments.stream().map(Environment::id).collect(Collectors.toSet());
    }

    public Set<String> filterServiceIds(tech.illuin.wombat.core.asset.Asset asset)
    {
        return this.environment(asset.identity().environmentId())
            .map(environment -> environment.assets().stream()
                .filter(filtered -> filtered.id().equals(asset.identity().id()))
                .flatMap(filtered -> filtered.serviceIds().stream())
                .collect(Collectors.toSet()))
            .orElseGet(Set::of);
    }

    public static AssetFilter none()
    {
        return new AssetFilter(Set.of());
    }

    public static AssetFilter of(String environmentId, Collection<String> assetIds)
    {
        Set<Asset> assets = assetIds.stream()
            .map(assetId -> new Asset(assetId, Set.of()))
            .collect(Collectors.toSet());

        return new AssetFilter(new Environment(environmentId, assets));
    }

    public record Environment(
        String id,
        Set<Asset> assets
    ) {
        public Set<String> assetIds()
        {
            return this.assets.stream().map(Asset::id).collect(Collectors.toSet());
        }
    }

    public record Asset(
        String id,
        Set<String> serviceIds
    ) {}
}
