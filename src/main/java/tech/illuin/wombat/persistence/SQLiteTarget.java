package tech.illuin.wombat.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.persistence.model.ContainerLocation;
import tech.illuin.wombat.persistence.model.DatapointEntity;
import tech.illuin.wombat.persistence.model.KubernetesPayload;
import tech.illuin.wombat.persistence.model.TimeRange;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SQLiteTarget implements LoadTarget
{

    private static final Logger logger = LoggerFactory.getLogger(SQLiteTarget.class);
    private static final String TYPE_KUBERNETES = "KUBERNETES";
    private static final TypeReference<List<KubernetesPayload>> PAYLOAD_TYPE = new TypeReference<>() {};

    private final DatapointRepository repository;
    private final ObjectMapper mapper;

    public SQLiteTarget(DatapointRepository repository, ObjectMapper mapper)
    {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void outputToTarget(Instant instant, List<PodMetrics> podMetricsList, String clusterId, String namespace)
    {
        long ms = instant.toEpochMilli();
        logger.debug("Persisting metrics for {} pods in {}/{}", podMetricsList.size(), clusterId, namespace);

        Map<String, Map<String, String>> allPodCpu = new HashMap<>();
        for (PodMetrics podMetrics : podMetricsList)
        {
            String podName = podMetrics.getMetadata().getName();
            Map<String, String> containerCpu = new HashMap<>();
            podMetrics.getContainers().forEach(cm -> {
                logger.trace("Current metrics {}", cm);
                var usage = cm.getUsage();
                if (usage == null || usage.get("cpu") == null)
                {
                    logger.warn("No CPU metric for container {} in pod {}, skipping", cm.getName(), podName);
                    containerCpu.put(cm.getName(), "0");
                }
                else {
                    logger.trace("Saving {}", cm);
                    containerCpu.put(cm.getName(), usage.get("cpu").getAmount());
                }
            });
            if (containerCpu.isEmpty())
                logger.warn("No CPU metrics available for pod {}, skipping", podName);
            else
                allPodCpu.put(podName, containerCpu);
        }

        if (allPodCpu.isEmpty())
        {
            logger.warn("No CPU metrics for any pod in {}/{}, skipping", clusterId, namespace);
            return;
        }

        this.repository.upsert(ms, TYPE_KUBERNETES, existingJson -> {
            List<KubernetesPayload> payloads = existingJson != null
                ? readPayload(existingJson)
                : new ArrayList<>();
            KubernetesPayload entry = payloads.stream()
                .filter(p -> p.clusterId().equals(clusterId) && p.namespace().equals(namespace))
                .findFirst()
                .orElseGet(() -> {
                    KubernetesPayload newEntry = new KubernetesPayload(clusterId, namespace, new HashMap<>());
                    payloads.add(newEntry);
                    return newEntry;
                });
            entry.pods().putAll(allPodCpu);
            logger.debug("Current payload {}", payloads);
            return serializePayload(payloads);
        });
    }

    @Override
    public LoadData computeLoad(TimeRange timeRange, List<String> clusterIds) throws NoCPUUsageException
    {
        List<DatapointEntity> datapoints = this.repository.findByTypeAndRange(
            TYPE_KUBERNETES, toEpochMs(timeRange.start()), toEpochMs(timeRange.end()));
        if (datapoints.isEmpty()) throw new NoCPUUsageException("Could not compute CPU Usage");

        Set<String> clusterIdSet = clusterIds.isEmpty() ? Set.of() : new HashSet<>(clusterIds);

        List<List<KubernetesPayload>> allPayloads = datapoints.stream()
            .map(dp -> filterPayloads(readPayload(dp.payload), clusterIdSet))
            .toList();

        double cpuUsage = allPayloads.stream()
            .mapToDouble(SQLiteTarget::totalCpu)
            .average()
            .orElseThrow(() -> new NoCPUUsageException("Could not compute CPU Usage"));

        Map<String, Double> totalCpuPerContainer = new HashMap<>();
        double grandTotal = 0;
        for (List<KubernetesPayload> payloads : allPayloads)
        {
            Map<String, Double> cpuByContainer = containerCpuSums(payloads);
            cpuByContainer.forEach((container, cpu) -> totalCpuPerContainer.merge(container, cpu, Double::sum));
            grandTotal += cpuByContainer.values().stream().mapToDouble(Double::doubleValue).sum();
        }
        final double total = grandTotal;
        Map<String, Double> containerShares = total == 0 ? Map.of()
            : totalCpuPerContainer.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue() / total
            ));

        Map<String, Set<ContainerLocation>> locationSets = new HashMap<>();
        for (List<KubernetesPayload> payloads : allPayloads)
        {
            for (KubernetesPayload payload : payloads)
            {
                ContainerLocation location = new ContainerLocation(payload.clusterId(), payload.namespace());
                payload.pods().values().forEach(containers ->
                    containers.keySet().forEach(name ->
                        locationSets.computeIfAbsent(name, k -> new LinkedHashSet<>()).add(location)));
            }
        }
        Map<String, List<ContainerLocation>> containerLocations = locationSets.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> List.copyOf(e.getValue())
            ));

        return new LoadData(cpuUsage, containerShares, containerLocations);
    }

    @Override
    public double computeCpuUsage(TimeRange timeRange, List<String> clusterIds) throws NoCPUUsageException
    {
        List<DatapointEntity> datapoints = this.repository.findByTypeAndRange(
            TYPE_KUBERNETES, toEpochMs(timeRange.start()), toEpochMs(timeRange.end()));
        if (datapoints.isEmpty()) throw new NoCPUUsageException("Could not compute CPU Usage");

        Set<String> clusterIdSet = clusterIds.isEmpty() ? Set.of() : new HashSet<>(clusterIds);
        return datapoints.stream()
            .mapToDouble(dp -> totalCpu(filterPayloads(readPayload(dp.payload), clusterIdSet)))
            .average()
            .orElseThrow(() -> new NoCPUUsageException("Could not compute CPU Usage"));
    }

    @Override
    public Map<String, Double> getContainerShares(TimeRange timeRange, List<String> clusterIds) throws NoCPUUsageException
    {
        List<DatapointEntity> datapoints = this.repository.findByTypeAndRange(
            TYPE_KUBERNETES, toEpochMs(timeRange.start()), toEpochMs(timeRange.end()));
        if (datapoints.isEmpty()) return Map.of();

        Set<String> clusterIdSet = clusterIds.isEmpty() ? Set.of() : new HashSet<>(clusterIds);
        Map<String, Double> totalCpuPerContainer = new HashMap<>();
        double grandTotal = 0;
        for (DatapointEntity dp : datapoints)
        {
            List<KubernetesPayload> payloads = filterPayloads(readPayload(dp.payload), clusterIdSet);
            Map<String, Double> cpuByContainer = containerCpuSums(payloads);
            cpuByContainer.forEach((container, cpu) -> totalCpuPerContainer.merge(container, cpu, Double::sum));
            grandTotal += cpuByContainer.values().stream().mapToDouble(Double::doubleValue).sum();
        }

        if (grandTotal == 0) return Map.of();
        final double total = grandTotal;
        return totalCpuPerContainer.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> e.getValue() / total
        ));
    }

    @Override
    public Map<String, List<ContainerLocation>> getContainerLocations(TimeRange timeRange, List<String> clusterIds)
    {
        List<DatapointEntity> datapoints = this.repository.findByTypeAndRange(
            TYPE_KUBERNETES, toEpochMs(timeRange.start()), toEpochMs(timeRange.end()));
        if (datapoints.isEmpty()) return Map.of();

        Set<String> clusterIdSet = clusterIds.isEmpty() ? Set.of() : new HashSet<>(clusterIds);
        Map<String, Set<ContainerLocation>> locations = new HashMap<>();
        for (DatapointEntity dp : datapoints)
        {
            for (KubernetesPayload payload : filterPayloads(readPayload(dp.payload), clusterIdSet))
            {
                ContainerLocation location = new ContainerLocation(payload.clusterId(), payload.namespace());
                payload.pods().values().forEach(containers ->
                    containers.keySet().forEach(name ->
                        locations.computeIfAbsent(name, k -> new LinkedHashSet<>()).add(location)));
            }
        }
        return locations.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> List.copyOf(e.getValue())
        ));
    }

    private static List<KubernetesPayload> filterPayloads(List<KubernetesPayload> payloads, Set<String> clusterIds)
    {
        if (clusterIds.isEmpty()) return payloads;
        return payloads.stream()
            .filter(p -> clusterIds.contains(p.clusterId()))
            .collect(Collectors.toList());
    }

    private List<KubernetesPayload> readPayload(String json)
    {
        try
        {
            JsonNode node = this.mapper.readTree(json);
            if (node.isArray())
                return this.mapper.readerFor(PAYLOAD_TYPE).readValue(node);
            // legacy rows stored a single object before the list format was introduced
            return new ArrayList<>(List.of(this.mapper.treeToValue(node, KubernetesPayload.class)));
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to deserialize datapoint payload", e);
        }
    }

    private String serializePayload(List<KubernetesPayload> payloads)
    {
        try
        {
            return this.mapper.writeValueAsString(payloads);
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to serialize payload", e);
        }
    }

    private static double totalCpu(List<KubernetesPayload> payloads)
    {
        return payloads.stream()
            .flatMap(p -> p.pods().values().stream())
            .flatMap(containers -> containers.values().stream())
            .mapToDouble(Double::parseDouble)
            .sum();
    }

    private static Map<String, Double> containerCpuSums(List<KubernetesPayload> payloads)
    {
        Map<String, Double> result = new HashMap<>();
        payloads.forEach(payload ->
            payload.pods().values().forEach(containers ->
                containers.forEach((name, cpu) -> result.merge(name, Double.parseDouble(cpu), Double::sum))));
        return result;
    }

    private static long toEpochMs(Instant instant)
    {
        if (instant.equals(Instant.MIN)) return Long.MIN_VALUE;
        if (instant.equals(Instant.MAX)) return Long.MAX_VALUE;
        return instant.toEpochMilli();
    }
}
