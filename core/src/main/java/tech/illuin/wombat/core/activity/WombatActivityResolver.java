package tech.illuin.wombat.core.activity;

import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.commons.AssetFilter;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.module.AssetProcessor;

public interface WombatActivityResolver extends AssetProcessor
{
    ActivityData resolve(Asset asset, TimeRange range, AssetFilter filter) throws WombatActivityException;
}
