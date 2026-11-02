package tech.illuin.wombat.module.kubernetes_static.activity;

import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.commons.AssetActivity;
import tech.illuin.wombat.core.activity.commons.AssetFilter;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.activity.kubernetes.CPUUsage;
import tech.illuin.wombat.core.activity.kubernetes.KubernetesActivityData;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.evaluation.impact.kubernetes.ClusterInfo;
import tech.illuin.wombat.module.kubernetes_static.KubernetesStaticAsset;
import tech.illuin.wombat.module.kubernetes_static.KubernetesStaticProfile;
import tech.illuin.wombat.module.kubernetes_static.scenario.evaluator.CompositeScenarioEvaluator;
import tech.illuin.wombat.module.kubernetes_static.scenario.evaluator.ScenarioEvaluator;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class KubernetesStaticActivityResolver implements WombatActivityResolver
{
    private final ScenarioEvaluator evaluator;

    public KubernetesStaticActivityResolver()
    {
        this(new CompositeScenarioEvaluator());
    }

    public KubernetesStaticActivityResolver(ScenarioEvaluator evaluator)
    {
        this.evaluator = evaluator;
    }

    @Override
    public boolean accept(Asset asset)
    {
        return asset instanceof KubernetesStaticAsset;
    }

    @Override
    public Optional<AssetActivity> resolve(Asset asset, TimeRange range, AssetFilter filter)
    {
        KubernetesStaticAsset staticAsset = (KubernetesStaticAsset) asset;
        KubernetesStaticProfile profile = staticAsset.profile();
        Set<String> serviceIds = filter == null ? Collections.emptySet() : filter.filterServiceIds(asset);

        String assetId = asset.identity().id();
        Set<String> resolvedServiceIds = serviceIds.isEmpty() ? Set.of(assetId) : serviceIds;
        Map<String, Double> containerShares = Map.of(assetId, 1.0);
        Map<String, ClusterInfo> containerLocations = Map.of(assetId, new ClusterInfo(assetId, "default"));

        ZoneId zone = profile.zoneId() != null ? profile.zoneId() : ZoneOffset.UTC;

        List<ActivityData> activities = new ArrayList<>();
        for (TimeRange slot : range.split())
        {
            Duration step = resolveSamplingStep(slot.resolution());
            List<TimeRange> subSlots = slot.split(step);

            double totalLoad = 0.0;
            for (TimeRange subSlot : subSlots)
            {
                Instant midpoint = subSlot.start().plus(subSlot.resolution().dividedBy(2));
                ZonedDateTime zonedDateTime = midpoint.atZone(zone);
                totalLoad += this.evaluator.evaluate(zonedDateTime, profile.scenarios());
            }

            double meanLoadFactor = subSlots.isEmpty() ? 0.0 : totalLoad / subSlots.size();

            activities.add(new KubernetesActivityData(
                ActivityRegime.MODELED,
                resolvedServiceIds,
                slot,
                CPUUsage.relative(meanLoadFactor),
                containerShares,
                containerLocations
            ));
        }

        return Optional.of(new AssetActivity(asset.identity(), activities));
    }

    private static Duration resolveSamplingStep(Duration slotDuration)
    {
        if (slotDuration.compareTo(Duration.ofDays(1)) >= 0)
            return Duration.ofHours(3);
        if (slotDuration.compareTo(Duration.ofHours(1)) >= 0)
            return Duration.ofHours(1);
        return slotDuration;
    }
}
