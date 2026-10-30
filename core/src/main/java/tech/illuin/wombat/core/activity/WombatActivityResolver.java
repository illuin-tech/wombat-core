package tech.illuin.wombat.core.activity;

import tech.illuin.wombat.core.activity.commons.AssetActivity;
import tech.illuin.wombat.core.activity.commons.AssetFilter;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.module.AssetProcessor;

import java.util.Optional;

public interface WombatActivityResolver extends AssetProcessor
{
    Optional<AssetActivity> resolve(Asset asset, TimeRange range, AssetFilter filter) throws WombatActivityException;
}
