package tech.illuin.wombat.core.evaluation.impact.kubernetes;

import io.micrometer.core.instrument.MeterRegistry;
import tech.illuin.wombat.core.source.persistence.micrometer.data.Aggregation;
import tech.illuin.wombat.core.source.persistence.micrometer.data.MetricGroup;
import tech.illuin.wombat.core.source.persistence.micrometer.data.TagGroup;

import java.util.*;

import static tech.illuin.wombat.core.source.persistence.micrometer.MicrometerTags.*;

public class MicrometerKubernetesMetricResolver implements KubernetesMetricResolver
{
    private final MeterRegistry registry;

    private static final Map<String, Aggregation> VALUES = Map.of(
        METRIC_K8S_CPU, Aggregation.MEAN,
        METRIC_K8S_RAM, Aggregation.MEAN
    );

    private static final Set<String> TAGS = Set.of(
        TAG_ENVIRONMENT, TAG_ASSET, TAG_ASSET_NAME, TAG_ASSET_TYPE,
        TAG_SERVICE, TAG_K8S_CLUSTER, TAG_K8S_NAMESPACE, TAG_K8S_POD, TAG_K8S_CONTAINER
    );

    public MicrometerKubernetesMetricResolver(MeterRegistry registry)
    {
        this.registry = registry;
    }

    @Override
    public Optional<Double> averageCpuPerInstant(long startMs, long endMs, List<String> assetIds)
    {
        List<MetricGroup> groups = gatherGroups(assetIds);
        List<Double> cpus = groups.stream()
            .map(g -> g.value(METRIC_K8S_CPU))
            .flatMap(Optional::stream)
            .toList();

        if (cpus.isEmpty())
            return Optional.empty();

        double totalCpu = cpus.stream()
            .mapToDouble(Double::doubleValue)
            .sum();

        return Optional.of(totalCpu);
    }

    @Override
    public Map<String, Double> containerShares(long startMs, long endMs, List<String> assetIds)
    {
        List<MetricGroup> groups = gatherGroups(assetIds);
        Map<String, Double> cpuByContainer = new LinkedHashMap<>();
        double totalCpu = 0.0;

        for (MetricGroup group : groups)
        {
            String serviceId = group.tag(TAG_SERVICE).orElse(null);
            Optional<Double> cpu = group.value(METRIC_K8S_CPU);
            if (serviceId != null && cpu.isPresent())
            {
                cpuByContainer.merge(serviceId, cpu.get(), Double::sum);
                totalCpu += cpu.get();
            }
        }

        if (totalCpu <= 0.0)
            return Map.of();

        Map<String, Double> shares = new LinkedHashMap<>();
        for (var entry : cpuByContainer.entrySet())
            shares.put(entry.getKey(), entry.getValue() / totalCpu);

        return shares;
    }

    @Override
    public Map<String, List<ContainerLocation>> containerLocations(long startMs, long endMs, List<String> assetIds)
    {
        List<MetricGroup> groups = gatherGroups(assetIds);
        Map<String, Set<ContainerLocation>> locations = new LinkedHashMap<>();

        for (MetricGroup group : groups)
        {
            String serviceId = group.tag(TAG_SERVICE).orElse(null);
            String cluster = group.tag(TAG_K8S_CLUSTER).orElse(null);
            String namespace = group.tag(TAG_K8S_NAMESPACE).orElse(null);

            if (serviceId != null && cluster != null && namespace != null)
            {
                locations.computeIfAbsent(serviceId, k -> new LinkedHashSet<>())
                    .add(new ContainerLocation(cluster, namespace));
            }
        }

        Map<String, List<ContainerLocation>> result = new LinkedHashMap<>();
        for (var entry : locations.entrySet())
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));

        return result;
    }

    @Override
    public Map<Long, Double> cpuTimePerBucket(long startMs, long endMs, long stepMs, List<String> assetIds, Collection<String> serviceIds)
    {
        List<MetricGroup> groups = gatherGroups(assetIds);
        long bucket = (startMs / stepMs) * stepMs;
        long spanMs = Math.max(0, endMs - startMs);

        double totalCpuTime = groups.stream()
            .filter(g -> serviceIds.isEmpty() || serviceIds.contains(g.tag(TAG_SERVICE).orElse("")))
            .map(g -> g.value(METRIC_K8S_CPU))
            .flatMap(Optional::stream)
            .mapToDouble(cpu -> cpu * spanMs)
            .sum();

        return totalCpuTime > 0.0 ? Map.of(bucket, totalCpuTime) : Map.of();
    }

    private List<MetricGroup> gatherGroups(List<String> assetIds)
    {
        Map<TagGroup, MetricGroup> groups = new HashMap<>();
        for (var meter : this.registry.getMeters())
        {
            TagGroup.from(meter.getId(), TAGS)
                .filter(tg -> assetIds.isEmpty() || assetIds.contains(tg.get(TAG_ASSET)))
                .map(tg -> groups.computeIfAbsent(tg, MetricGroup::new))
                .ifPresent(mg -> mg.recordValues(meter, VALUES));
        }
        return new ArrayList<>(groups.values());
    }
}
