package tech.illuin.wombat.core.activity.commons;

import tech.illuin.wombat.core.asset.AssetIdentity;

import java.util.List;

public record AssetActivity(
    AssetIdentity assetIdentity,
    List<ActivityData> activity
) {}
