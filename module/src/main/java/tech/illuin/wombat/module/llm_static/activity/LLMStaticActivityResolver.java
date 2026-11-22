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
import tech.illuin.wombat.module.llm_static.scenario.Scenario;
import tech.illuin.wombat.module.llm_static.scenario.evaluator.CompositeScenarioEvaluator;
import tech.illuin.wombat.module.llm_static.scenario.evaluator.ScenarioEvaluator;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LLMStaticActivityResolver implements WombatActivityResolver
{
    private final ScenarioEvaluator evaluator;
    /* Per model, as its scenarios set it once and for all */
    private final Map<LLMStaticProfile.ModelConfig, Double> areas = new ConcurrentHashMap<>();

    /* Any non-leap year: only its months' lengths and its days' local times are read */
    private static final Year REFERENCE_YEAR = Year.of(2026);

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

                double area = this.areas.computeIfAbsent(modelConfig, config -> this.averageYearArea(config.scenarios()));
                /* Requests stay fractional: a low volume rounded per slot would drift from its annual total */
                double requestCount = 0.0;
                if (area > 0.0)
                {
                    for (TimeRange subSlot : subSlots)
                    {
                        Instant midpoint = subSlot.start().plus(subSlot.resolution().dividedBy(2));
                        double shape = this.evaluator.evaluate(midpoint.atZone(zone), modelConfig.scenarios());
                        requestCount += modelConfig.requestPerYear() * shape * hours(subSlot.resolution()) / area;
                    }
                }
                long outputTokenCount = Math.round(requestCount * modelConfig.outputTokenPerRequest());

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

    /**
     * The area under the scenarios' shape over an average 365-day year, in level-hours: for each month and weekday, a day
     * sampled at each hour's midpoint, as the slots are, weighted by the days that month holds, spread evenly over the
     * weekdays. One scale for all time, whatever a given year's calendar.
     */
    private double averageYearArea(List<Scenario> scenarios)
    {
        double area = 0.0;
        for (Month month : Month.values())
        {
            double monthArea = 0.0;
            for (DayOfWeek dayOfWeek : DayOfWeek.values())
            {
                LocalDate day = REFERENCE_YEAR.atMonth(month).atDay(1).with(TemporalAdjusters.nextOrSame(dayOfWeek));
                for (int hour = 0; hour < 24; hour++)
                    monthArea += this.evaluator.evaluate(day.atTime(hour, 30).atZone(ZoneOffset.UTC), scenarios);
            }
            area += monthArea * month.length(false) / DayOfWeek.values().length;
        }
        return area;
    }

    private static double hours(Duration duration)
    {
        return duration.toNanos() / (double) Duration.ofHours(1).toNanos();
    }
}
