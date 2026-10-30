package tech.illuin.wombat.core.activity.kubernetes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.core.activity.WombatActivityException;
import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.commons.AssetActivity;
import tech.illuin.wombat.core.activity.commons.AssetFilter;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.evaluation.impact.kubernetes.ClusterInfo;
import tech.illuin.wombat.core.evaluation.impact.kubernetes.ContainerLocation;
import tech.illuin.wombat.core.evaluation.impact.kubernetes.KubernetesMetricResolver;

import java.util.*;

import static tech.illuin.wombat.core.activity.commons.TimeRange.toEpochMs;

public class KubernetesActivityResolver implements WombatActivityResolver
{
    private final KubernetesMetricResolver metricResolver;

    private static final Logger logger = LoggerFactory.getLogger(KubernetesActivityResolver.class);

    public KubernetesActivityResolver(KubernetesMetricResolver metricResolver)
    {
        this.metricResolver = metricResolver;
    }

    @Override
    public boolean accept(Asset asset)
    {
        return asset.type().family() == ServiceFamily.KUBERNETES_CONTAINER;
    }

    @Override
    public Optional<AssetActivity> resolve(Asset asset, TimeRange range, AssetFilter filter) throws WombatActivityException
    {
        List<String> assetIds = List.of(asset.identity().id());
        Set<String> serviceIds = filter == null ? Set.of() : filter.filterServiceIds(asset);

        List<ActivityData> activities = range.split().stream()
            .map(slot -> resolveSlot(asset, slot, serviceIds, assetIds))
            .flatMap(Optional::stream)
            .toList();

        return activities.isEmpty()
            ? Optional.empty()
            : Optional.of(new AssetActivity(asset.identity(), activities));
    }

    private Optional<ActivityData> resolveSlot(Asset asset, TimeRange range, Set<String> serviceIds, List<String> assetIds)
    {
        long start = toEpochMs(range.start());
        long end = toEpochMs(range.end());

        return this.metricResolver.averageCpuPerInstant(start, end, assetIds)
            .map(cpuUsage -> {
                Map<String, Double> containerShares = this.metricResolver.containerShares(start, end, assetIds);
                Map<String, ClusterInfo> containerLocations = toClusterInfo(this.metricResolver.containerLocations(start, end, assetIds));

                return new KubernetesActivityData(
                    asset.type().regime(),
                    serviceIds,
                    range,
                    cpuUsage,
                    containerShares,
                    containerLocations
                );
            });
    }

    private static Map<String, ClusterInfo> toClusterInfo(Map<String, List<ContainerLocation>> locations)
    {
        Map<String, ClusterInfo> mapped = new LinkedHashMap<>();
        locations.forEach((containerId, containerLocations) -> {
            if (containerLocations.isEmpty())
                return;
            if (containerLocations.size() > 1)
                logger.warn("Container {} reported {} locations, keeping the first one", containerId, containerLocations.size());

            ContainerLocation location = containerLocations.getFirst();
            mapped.put(containerId, new ClusterInfo(location.clusterId(), location.namespace()));
        });
        return mapped;
    }
}
