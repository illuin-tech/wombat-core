package tech.illuin.wombat.core.asset;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import tech.illuin.wombat.core.asset.type.AssetType;

import java.util.Objects;

/**
 * Identifies an asset, and names its {@link AssetType}. Embedded in an {@link Asset}, its {@code type} is the asset's
 * own {@code type} property, which also selects the asset's class.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AssetIdentity(
    @NotBlank @JsonProperty("id") String id,
    @NotBlank @JsonProperty("environmentId") @JsonAlias("environment-id") String environmentId,
    @NotBlank @JsonProperty("type") String assetType
) {
    public AssetIdentity
    {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(environmentId, "environmentId must not be null");
        Objects.requireNonNull(assetType, "assetType must not be null");
        if (id.isBlank())
            throw new IllegalArgumentException("id must not be blank");
        if (environmentId.isBlank())
            throw new IllegalArgumentException("environmentId must not be blank");
        if (assetType.isBlank())
            throw new IllegalArgumentException("assetType must not be blank");
    }

    public static AssetIdentity of(String id, String environmentId, String assetType)
    {
        return new AssetIdentity(id, environmentId, assetType);
    }

    public static AssetIdentity of(String id, String environmentId, AssetType assetType)
    {
        return new AssetIdentity(id, environmentId, assetType.name());
    }
}
