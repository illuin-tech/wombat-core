package tech.illuin.wombat.core.asset;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tech.illuin.wombat.core.asset.profile.Profile;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
public interface Asset
{
    String id();

    String environmentId();

    String name();

    @JsonIgnore
    AssetType type();

    Profile profile();
}
