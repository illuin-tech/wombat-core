package tech.illuin.vigilantwombat.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.vigilantwombat.persistence.model.DatapointEntity;
import tech.illuin.vigilantwombat.persistence.model.TimeRange;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MemoryLoadTarget implements LoadTarget {

    private static final Logger logger = LoggerFactory.getLogger(MemoryLoadTarget.class);
    private static final String TYPE_KUBERNETES = "KUBERNETES";
    private static final TypeReference<Map<String, Map<String, String>>> PAYLOAD_TYPE = new TypeReference<>() {};

    private final DatapointRepository repository;
    private final ObjectMapper mapper;

    public MemoryLoadTarget(DatapointRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void outputToTarget(Instant instant, PodMetrics podMetrics, String namespace) {
        long ms = instant.toEpochMilli();
        String podName = podMetrics.getMetadata().getName();
        logger.info("Persisting metrics for pod {}", podName);

        Map<String, String> containerCpu = new HashMap<>();
        podMetrics.getContainers().forEach(cm -> {
            logger.info("Current metrics {}", cm);
            var usage = cm.getUsage();
            if (usage == null || usage.get("cpu") == null) {
                logger.warn("No CPU metric for container {} in pod {}, skipping", cm.getName(), podName);
                containerCpu.put(cm.getName(), "0");
            } else {
                logger.trace("Saving {}", cm);
                containerCpu.put(cm.getName(), usage.get("cpu").getAmount());
            }
        });
        if (containerCpu.isEmpty()) {
            logger.warn("No CPU metrics available for pod {}, skipping", podName);
            return;
        }

        try {
            this.repository.upsert(ms, TYPE_KUBERNETES, existingJson -> {
                Map<String, Map<String, String>> payload = existingJson != null
                    ? readPayload(existingJson)
                    : new HashMap<>();
                payload.put(podName, containerCpu);
                logger.info("Current payload {}", payload);
                try {
                    return mapper.writeValueAsString(payload);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to serialize payload for pod " + podName, e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist pod metrics for " + podName, e);
        }
    }

    @Override
    public double computeCpuUsage(TimeRange timeRange) throws NoCPUUsageException {
        List<DatapointEntity> datapoints = this.repository.findByTypeAndRange(
            TYPE_KUBERNETES, toEpochMs(timeRange.start()), toEpochMs(timeRange.end()));
        if (datapoints.isEmpty()) throw new NoCPUUsageException("Could not compute CPU Usage");

        return datapoints.stream()
            .mapToDouble(dp -> totalCpu(readPayload(dp.payload)))
            .average()
            .orElseThrow(() -> new NoCPUUsageException("Could not compute CPU Usage"));
    }

    @Override
    public Map<String, Double> getContainerShares(TimeRange timeRange) throws NoCPUUsageException {
        List<DatapointEntity> datapoints = this.repository.findByTypeAndRange(
            TYPE_KUBERNETES, toEpochMs(timeRange.start()), toEpochMs(timeRange.end()));
        if (datapoints.isEmpty()) return Map.of();

        Map<String, Double> totalCpuPerContainer = new HashMap<>();
        double grandTotal = 0;
        for (DatapointEntity dp : datapoints) {
            Map<String, Map<String, String>> payload = readPayload(dp.payload);
            Map<String, Double> cpuByContainer = containerCpuSums(payload);
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

    private Map<String, Map<String, String>> readPayload(String json) {
        try {
            return mapper.readValue(json, PAYLOAD_TYPE);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize datapoint payload", e);
        }
    }

    private static double totalCpu(Map<String, Map<String, String>> payload) {
        return payload.values().stream()
            .flatMap(containers -> containers.values().stream())
            .mapToDouble(Double::parseDouble)
            .sum();
    }

    private static Map<String, Double> containerCpuSums(Map<String, Map<String, String>> payload) {
        Map<String, Double> result = new HashMap<>();
        payload.values().forEach(containers ->
            containers.forEach((name, cpu) -> result.merge(name, Double.parseDouble(cpu), Double::sum)));
        return result;
    }

    private static long toEpochMs(Instant instant) {
        if (instant.equals(Instant.MIN)) return Long.MIN_VALUE;
        if (instant.equals(Instant.MAX)) return Long.MAX_VALUE;
        return instant.toEpochMilli();
    }
}
