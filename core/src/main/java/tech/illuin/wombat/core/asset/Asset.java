package tech.illuin.wombat.core.asset;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.asset.profile.AssetProfile;
import tech.illuin.wombat.core.asset.type.AssetType;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
public interface Asset
{
    @NotNull @Valid
    AssetIdentity identity();

    @JsonIgnore
    AssetType type();

    @NotNull
    AssetProfile profile();

    /**
     * Required of the assets whose module registers a {@link tech.illuin.wombat.core.evaluation.WombatProfileResolver},
     * which they are evaluated with the profile of.
     *
     * @return this asset with {@code profile} in place of its own
     * @throws UnsupportedOperationException when this asset's profile cannot be replaced
     * @throws IllegalArgumentException when this asset cannot hold {@code profile}
     */
    default Asset withProfile(AssetProfile profile)
    {
        throw new UnsupportedOperationException("Asset-type " + this.type().name() + " cannot have its profile replaced");
    }
}
