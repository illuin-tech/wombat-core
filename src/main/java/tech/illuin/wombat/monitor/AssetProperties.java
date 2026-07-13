package tech.illuin.wombat.monitor;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface AssetProperties
{
    String id();

    String name();

    AssetType type();
}
