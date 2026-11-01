package tech.illuin.wombat.module.kubernetes_static.activity;

import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.commons.AssetActivity;
import tech.illuin.wombat.core.activity.commons.AssetFilter;
import tech.illuin.wombat.core.activity.commons.TimeRange;
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
    public static final double NANOCORES_PER_CORE = 1_000_000_000.0;

    private final ScenarioEvaluator evaluator;

    public KubernetesStaticActivityResolver()
    {
        this(new CompositeScenarioEvaluator());
    }

    public KubernetesStaticActivityResolver(ScenarioEvaluator evaluator)
    {
        this.evaluator = evaluator != null ? evaluator : new CompositeScenarioEvaluator();
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
            Duration slotDuration = slot.resolution();
            Instant midpoint = slot.start().plus(slotDuration.dividedBy(2));
            ZonedDateTime zonedDateTime = midpoint.atZone(zone);

            double loadFactor = this.evaluator.evaluate(zonedDateTime, profile.scenarios());
            double cpuNanocores = loadFactor * profile.nodeCount() * NANOCORES_PER_CORE;

            activities.add(new KubernetesActivityData(
                ActivityRegime.MODELED,
                resolvedServiceIds,
                slot,
                cpuNanocores,
                containerShares,
                containerLocations
            ));
        }

        return Optional.of(new AssetActivity(asset.identity(), activities));
    }
}
