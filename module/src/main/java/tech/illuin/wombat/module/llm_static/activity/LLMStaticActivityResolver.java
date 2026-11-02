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
import java.time.ZoneOffset;
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
    private static final Duration ONE_YEAR = Duration.ofDays(365);

    private final ScenarioEvaluator evaluator;

    public LLMStaticActivityResolver()
    {
        this(new CompositeScenarioEvaluator());
    }

    public LLMStaticActivityResolver(ScenarioEvaluator evaluator)
    {
        this.evaluator = evaluator != null ? evaluator : new CompositeScenarioEvaluator();
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
            Duration slotDuration = slot.resolution();
            Instant midpoint = slot.start().plus(slotDuration.dividedBy(2));

            Map<String, LLMServiceActivity> serviceActivities = new LinkedHashMap<>();
            for (LLMStaticProfile.ModelConfig modelConfig : profile.models())
            {
                String serviceId = modelConfig.model();
                if (!serviceIds.isEmpty() && !serviceIds.contains(serviceId))
                    continue;

                ZoneId zone = modelConfig.zoneId() != null
                    ? modelConfig.zoneId()
                    : (profile.zoneId() != null ? profile.zoneId() : ZoneOffset.UTC);

                ZonedDateTime zonedDateTime = midpoint.atZone(zone);
                ScenarioResult result = this.evaluator.evaluate(zonedDateTime, modelConfig.scenarios());

                int requestCount = requestCountOver(result.requestPerYear(), slotDuration);
                long outputTokenCount = (long) result.outputTokenPerRequest() * requestCount;

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

    private static int requestCountOver(double requestsPerYear, Duration duration)
    {
        return (int) (requestsPerYear * ((double) duration.toSeconds() / ONE_YEAR.toSeconds()));
    }
}
