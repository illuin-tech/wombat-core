package tech.illuin.wombat.core.asset;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record Environment(
    @NotBlank String id,
    List<@Valid @NotNull Asset> assets
) {
    public Asset get(String assetId)
    {
        return this.assets.stream()
            .filter(a -> a.id().equals(assetId))
            .findFirst()
            .orElseThrow();
    }
}
