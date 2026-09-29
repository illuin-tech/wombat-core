package tech.illuin.wombat.core.activity.kubernetes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.core.activity.WombatActivityException;
import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.commons.AssetFilter;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.evaluation.impact.kubernetes.ClusterInfo;
import tech.illuin.wombat.core.evaluation.impact.kubernetes.ContainerLocation;
import tech.illuin.wombat.core.evaluation.impact.kubernetes.KubernetesMetricResolver;

import java.util.*;
import java.util.stream.Collectors;

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
    public Optional<ActivityData> resolve(Asset asset, TimeRange range, AssetFilter filter) throws WombatActivityException
    {
        List<String> assetIds = List.of(asset.identity().id());
        Set<String> serviceIds = serviceIds(asset, filter);
        long start = toEpochMs(range.start());
        long end = toEpochMs(range.end());

        return this.metricResolver.averageCpuPerInstant(start, end, assetIds)
            .map(cpuUsage -> {
                Map<String, Double> containerShares = this.metricResolver.containerShares(start, end, assetIds);
                Map<String, ClusterInfo> containerLocations = toClusterInfo(this.metricResolver.containerLocations(start, end, assetIds));

                return (ActivityData) new KubernetesActivityData(asset.type().regime(), serviceIds, range, cpuUsage, containerShares, containerLocations);
            })
            .or(Optional::empty);
    }

    private static Set<String> serviceIds(Asset asset, AssetFilter filter)
    {
        if (filter == null)
            return Set.of();

        return filter.environment(asset.identity().environmentId())
            .map(environment -> environment.assets().stream()
                .filter(filtered -> filtered.id().equals(asset.identity().id()))
                .flatMap(filtered -> filtered.serviceIds().stream())
                .collect(Collectors.toSet()))
            .orElseGet(Set::of);
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
