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

    /**
     * The asset's profile. An implementation marks it {@code @Valid} where it declares it, e.g. on its record component:
     * the validator does not cascade through this accessor, which is not a JavaBean getter.
     */
    @NotNull
    AssetProfile profile();
}
