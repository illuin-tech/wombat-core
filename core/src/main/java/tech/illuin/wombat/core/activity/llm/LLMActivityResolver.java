package tech.illuin.wombat.core.activity.llm;

import tech.illuin.wombat.core.activity.WombatActivityException;
import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.commons.AssetActivity;
import tech.illuin.wombat.core.activity.commons.AssetFilter;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.evaluation.impact.llm.LLMMetricResolver;

import java.util.*;

import static tech.illuin.wombat.core.activity.commons.TimeRange.toEpochMs;

public class LLMActivityResolver implements WombatActivityResolver
{
    private final LLMMetricResolver metricResolver;

    public LLMActivityResolver(LLMMetricResolver metricResolver)
    {
        this.metricResolver = metricResolver;
    }

    @Override
    public boolean accept(Asset asset)
    {
        return asset.type().family() == ServiceFamily.LLM;
    }

    @Override
    public Optional<AssetActivity> resolve(Asset asset, TimeRange range, AssetFilter filter) throws WombatActivityException
    {
        Set<String> filteredServiceIds = filter == null ? Set.of() : filter.filterServiceIds(asset);

        List<TimeRange> slots = range.split();

        Map<Long, Map<String, LLMServiceActivity>> activitiesPerSlot = this.metricResolver.serviceActivitiesPerBucket(range, asset.identity().id());
        List<ActivityData> activities = slots.stream()
            .map(slot -> resolveSlot(asset, slot, activitiesPerSlot.getOrDefault(toEpochMs(slot.start()), Map.of()), filteredServiceIds))
            .flatMap(Optional::stream)
            .toList();

        return activities.isEmpty()
            ? Optional.empty()
            : Optional.of(new AssetActivity(asset.identity(), activities));
    }

    private static Optional<ActivityData> resolveSlot(Asset asset, TimeRange range, Map<String, LLMServiceActivity> activities, Set<String> filteredServiceIds)
    {
        if (!filteredServiceIds.isEmpty())
        {
            Map<String, LLMServiceActivity> filtered = new LinkedHashMap<>();
            activities.forEach((key, activity) -> {
                if (filteredServiceIds.contains(key) || filteredServiceIds.contains(activity.model()))
                    filtered.put(key, activity);
            });
            activities = filtered;
        }

        if (activities.isEmpty())
            return Optional.empty();

        Set<String> resolvedServiceIds = filteredServiceIds.isEmpty() ? activities.keySet() : filteredServiceIds;
        return Optional.of(new LLMActivityData(
            asset.type().regime(),
            resolvedServiceIds,
            range,
            activities
        ));
    }
}
