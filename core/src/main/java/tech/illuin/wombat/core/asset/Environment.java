package tech.illuin.wombat.core.asset;

import java.util.List;

public record Environment(
    String id,
    List<Asset> assets
) {
    public Asset get(String assetId)
    {
        return this.assets.stream()
            .filter(a -> a.id().equals(assetId))
            .findFirst()
            .orElseThrow();
    }
}
