package tech.illuin.wombat.module.llm_static.activity;

import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.commons.AssetActivity;
import tech.illuin.wombat.core.activity.commons.AssetFilter;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.activity.llm.LLMActivityData;
import tech.illuin.wombat.core.activity.llm.LLMServiceActivity;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.module.llm_static.LLMStaticAsset;
import tech.illuin.wombat.module.llm_static.LLMStaticProfile;
import tech.illuin.wombat.module.llm_static.scenario.ScenarioResult;
import tech.illuin.wombat.module.llm_static.scenario.evaluator.CompositeScenarioEvaluator;
import tech.illuin.wombat.module.llm_static.scenario.evaluator.ScenarioEvaluator;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class LLMStaticActivityResolver implements WombatActivityResolver
{
    private final ScenarioEvaluator evaluator;

    private static final Duration ONE_YEAR = Duration.ofDays(365);

    public LLMStaticActivityResolver()
    {
        this(new CompositeScenarioEvaluator());
    }

    public LLMStaticActivityResolver(ScenarioEvaluator evaluator)
    {
        this.evaluator = evaluator;
    }

    @Override
    public boolean accept(Asset asset)
    {
        return asset instanceof LLMStaticAsset;
    }

    @Override
    public Optional<AssetActivity> resolve(Asset asset, TimeRange range, AssetFilter filter)
    {
        LLMStaticProfile profile = (LLMStaticProfile) asset.profile();
        Set<String> serviceIds = filter == null ? Collections.emptySet() : filter.filterServiceIds(asset);

        List<ActivityData> activities = new ArrayList<>();
        for (TimeRange slot : range.split())
        {
            Duration step = resolveSamplingStep(slot.resolution());
            List<TimeRange> subSlots = slot.split(step);
            ZoneId zone = profile.zoneId();

            Map<String, LLMServiceActivity> serviceActivities = new LinkedHashMap<>();
            for (LLMStaticProfile.ModelConfig modelConfig : profile.models())
            {
                String serviceId = modelConfig.model();
                if (!serviceIds.isEmpty() && !serviceIds.contains(serviceId))
                    continue;

                double totalRequestPerYear = 0.0;
                double totalOutputTokenPerRequest = 0.0;

                for (TimeRange subSlot : subSlots)
                {
                    Instant midpoint = subSlot.start().plus(subSlot.resolution().dividedBy(2));
                    ZonedDateTime zonedDateTime = midpoint.atZone(zone);
                    ScenarioResult result = this.evaluator.evaluate(zonedDateTime, modelConfig.scenarios());

                    totalRequestPerYear += result.requestPerYear();
                    totalOutputTokenPerRequest += result.outputTokenPerRequest();
                }

                double meanRequestPerYear = subSlots.isEmpty() ? 0.0 : totalRequestPerYear / subSlots.size();
                double meanOutputTokenPerRequest = subSlots.isEmpty() ? 0.0 : totalOutputTokenPerRequest / subSlots.size();

                int requestCount = requestCountOver(meanRequestPerYear, slot.resolution());
                long outputTokenCount = (long) (meanOutputTokenPerRequest * requestCount);

                serviceActivities.put(serviceId, new LLMServiceActivity(
                    modelConfig.provider(),
                    modelConfig.model(),
                    modelConfig.location(),
                    outputTokenCount,
                    requestCount
                ));
            }

            Set<String> resolvedServiceIds = serviceIds.isEmpty() ? serviceActivities.keySet() : serviceIds;
            activities.add(new LLMActivityData(
                ActivityRegime.MODELED,
                resolvedServiceIds,
                slot,
                serviceActivities
            ));
        }

        return Optional.of(new AssetActivity(asset.identity(), activities));
    }

    private static Duration resolveSamplingStep(Duration slotResolution)
    {
        if (slotResolution.compareTo(Duration.ofHours(1)) >= 0)
            return Duration.ofHours(1);
        return slotResolution;
    }

    private static int requestCountOver(double requestsPerYear, Duration duration)
    {
        return (int) Math.round(requestsPerYear * ((double) duration.toSeconds() / ONE_YEAR.toSeconds()));
    }
}
