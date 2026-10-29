package tech.illuin.wombat.core.activity;

import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.commons.AssetFilter;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.module.AssetProcessor;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface WombatActivityResolver extends AssetProcessor
{
    Optional<ActivityData> resolve(Asset asset, TimeRange range, AssetFilter filter) throws WombatActivityException;

    default ActivityResolverType activityResolverType()
    {
        return ActivityResolverType.INTERNAL_STORAGE;
    }

    default Map<String, Map<Long, Double>> activityPerBucket(Asset asset, TimeRange range, long stepMs, Collection<String> serviceIds) throws WombatActivityException
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " does not provide per-bucket activity");
    }

    enum ActivityResolverType
    {
        INTERNAL_STORAGE,
        EXTERNAL_API
    }
}
