package tech.illuin.wombat.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.persistence.model.DatapointEntity;
import tech.illuin.wombat.persistence.model.KubernetesPayload;
import tech.illuin.wombat.persistence.model.TimeRange;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public void outputToTarget(Instant instant, PodMetrics podMetrics, String clusterId, String namespace)
    {
        long ms = instant.toEpochMilli();
        String podName = podMetrics.getMetadata().getName();
        logger.info("Persisting metrics for pod {}", podName);

        Map<String, String> containerCpu = new HashMap<>();
        podMetrics.getContainers().forEach(cm -> {
            logger.info("Current metrics {}", cm);
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
        {
            logger.warn("No CPU metrics available for pod {}, skipping", podName);
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
            entry.pods().put(podName, containerCpu);
            logger.info("Current payload {}", payloads);
            return serializePayload(payloads);
        });
    }

    @Override
    public double computeCpuUsage(TimeRange timeRange, List<String> clusterIds) throws NoCPUUsageException
    {
        List<DatapointEntity> datapoints = this.repository.findByTypeAndRange(
            TYPE_KUBERNETES, toEpochMs(timeRange.start()), toEpochMs(timeRange.end()));
        if (datapoints.isEmpty()) throw new NoCPUUsageException("Could not compute CPU Usage");

        return datapoints.stream()
            .mapToDouble(dp -> totalCpu(filterPayloads(readPayload(dp.payload), clusterIds)))
            .average()
            .orElseThrow(() -> new NoCPUUsageException("Could not compute CPU Usage"));
    }

    @Override
    public Map<String, Double> getContainerShares(TimeRange timeRange, List<String> clusterIds) throws NoCPUUsageException
    {
        List<DatapointEntity> datapoints = this.repository.findByTypeAndRange(
            TYPE_KUBERNETES, toEpochMs(timeRange.start()), toEpochMs(timeRange.end()));
        if (datapoints.isEmpty()) return Map.of();

        Map<String, Double> totalCpuPerContainer = new HashMap<>();
        double grandTotal = 0;
        for (DatapointEntity dp : datapoints)
        {
            List<KubernetesPayload> payloads = filterPayloads(readPayload(dp.payload), clusterIds);
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

    private static List<KubernetesPayload> filterPayloads(List<KubernetesPayload> payloads, List<String> clusterIds)
    {
        if (clusterIds == null || clusterIds.isEmpty()) return payloads;
        return payloads.stream()
            .filter(p -> clusterIds.contains(p.clusterId()))
            .collect(Collectors.toList());
    }

    private List<KubernetesPayload> readPayload(String json)
    {
        try
        {
            JsonNode node = mapper.readTree(json);
            if (node.isArray())
                return mapper.readValue(json, PAYLOAD_TYPE);
            // legacy rows stored a single object before the list format was introduced
            return new ArrayList<>(List.of(mapper.treeToValue(node, KubernetesPayload.class)));
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize datapoint payload", e);
        }
    }

    private String serializePayload(List<KubernetesPayload> payloads)
    {
        try
        {
            return mapper.writeValueAsString(payloads);
        }
        catch (JsonProcessingException e) {
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
