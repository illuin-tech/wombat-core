package tech.illuin.wombat.monitor;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
public record Environment(
    String name,
    List<AssetProperties> assets
)
{
    public AssetProperties get(String assetId)
    {
        return this.assets.stream()
            .filter(a -> a.id().equals(assetId))
            .findFirst()
            .orElseThrow();
    }
}
