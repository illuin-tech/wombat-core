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

        List<TimeRange> slots = range.split();

        Map<Long, Double> cpuPerSlot = this.metricResolver.averageCpuPerBucket(range, assetIds);
        if (cpuPerSlot.isEmpty())
            return Optional.empty();

        Map<Long, Map<String, Double>> sharesPerSlot = this.metricResolver.containerSharesPerBucket(range, assetIds);
        Map<String, ClusterInfo> containerLocations = toClusterInfo(this.metricResolver.containerLocations(range, assetIds));

        List<ActivityData> activities = slots.stream()
            .map(slot -> resolveSlot(asset, slot, serviceIds, cpuPerSlot, sharesPerSlot, containerLocations))
            .flatMap(Optional::stream)
            .toList();

        return activities.isEmpty()
            ? Optional.empty()
            : Optional.of(new AssetActivity(asset.identity(), activities));
    }

    private static Optional<ActivityData> resolveSlot(
        Asset asset,
        TimeRange range,
        Set<String> serviceIds,
        Map<Long, Double> cpuPerSlot,
        Map<Long, Map<String, Double>> sharesPerSlot,
        Map<String, ClusterInfo> containerLocations
    ) {
        long start = toEpochMs(range.start());

        return Optional.ofNullable(cpuPerSlot.get(start))
            .map(cpuUsage -> new KubernetesActivityData(
                asset.type().regime(),
                serviceIds,
                range,
                CPUUsage.nanocores(cpuUsage),
                sharesPerSlot.getOrDefault(start, Map.of()),
                containerLocations
            ));
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
