package tech.illuin.wombat.core.asset;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import tech.illuin.wombat.core.asset.profile.Profile;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
public interface Asset
{
    @NotBlank
    String id();

    @NotBlank
    String environmentId();

    @NotBlank
    String name();

    @JsonIgnore
    AssetType type();

    @Valid
    Profile profile();
}
