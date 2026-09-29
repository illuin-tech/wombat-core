package tech.illuin.wombat.core.asset;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.asset.profile.AssetProfile;
import tech.illuin.wombat.core.asset.type.AssetType;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
public interface Asset
{
    @NotNull @Valid
    AssetIdentity identity();

    @JsonIgnore
    AssetType type();

    @NotNull @Valid
    AssetProfile profile();
}
