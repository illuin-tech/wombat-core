package tech.illuin.wombat.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.ContainerMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.metrics.MetricRecorderService;
import tech.illuin.wombat.persistence.model.ContainerLocation;
import tech.illuin.wombat.persistence.model.DatapointEntity;
import tech.illuin.wombat.persistence.model.PodMetrics;
import tech.illuin.wombat.persistence.model.TimeRange;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SQLiteTarget implements LoadTarget
{

    private static final Logger logger = LoggerFactory.getLogger(SQLiteTarget.class);
    private static final String TYPE_KUBERNETES = "KUBERNETES";

    private final DatapointRepository repository;
    private final ObjectMapper mapper;
    private final MetricRecorderService recorder;

    public SQLiteTarget(DatapointRepository repository, ObjectMapper mapper, MetricRecorderService recorder)
    {
        this.repository = repository;
        this.mapper = mapper;
        this.recorder = recorder;
    }

    @Override
    public void outputToTarget(Instant instant, List<io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics> podMetricsList, String clusterId, String namespace)
    {
        logger.debug("Recording metrics for {} pods in {}/{}", podMetricsList.size(), clusterId, namespace);
        for (io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics podMetrics : podMetricsList)
        {
            String podName = podMetrics.getMetadata().getName();
            for (ContainerMetrics cm : podMetrics.getContainers())
            {
                logger.trace("Current metrics {}", cm);
                var usage = cm.getUsage();
                if (usage == null || usage.get("cpu") == null)
                {
                    logger.warn("No CPU metric for container {} in pod {}, recording 0", cm.getName(), podName);
                    this.recorder.recordContainerCpu(clusterId, namespace, podName, cm.getName(), 0.0);
                    continue;
                }
                String amount = usage.get("cpu").getAmount();
                double cpu;
                try
                {
                    cpu = Double.parseDouble(amount);
                }
                catch (NumberFormatException e) {
                    logger.warn("CPU value {} for container {} in pod {} is not a parseable double, recording 0", amount, cm.getName(), podName);
                    cpu = 0.0;
                }
                this.recorder.recordContainerCpu(clusterId, namespace, podName, cm.getName(), cpu);
            }
        }
    }

    @Override
    public LoadData computeLoad(TimeRange timeRange, List<String> clusterIds) throws NoCPUUsageException
    {
        List<DatapointEntity> datapoints = fetch(timeRange, clusterIds);
        if (datapoints.isEmpty()) throw new NoCPUUsageException("Could not compute CPU Usage");

        Map<Long, Double> totalCpuPerInstant = new HashMap<>();
        Map<String, Double> totalCpuPerContainer = new HashMap<>();
        Map<String, Set<ContainerLocation>> locationSets = new HashMap<>();

        for (DatapointEntity dp : datapoints)
        {
            PodMetrics podMetrics = this.readPayload(dp.payload);
            double rowTotal = 0;
            for (PodMetrics.ContainerMetrics cm : podMetrics.pods().values())
            {
                for (Map.Entry<String, String> c : cm.containers().entrySet())
                {
                    double cpu = Double.parseDouble(c.getValue());
                    rowTotal += cpu;
                    totalCpuPerContainer.merge(c.getKey(), cpu, Double::sum);
                }
            }
            totalCpuPerInstant.merge(dp.instantMs, rowTotal, Double::sum);

            ContainerLocation location = new ContainerLocation(dp.cluster, dp.namespace);
            for (PodMetrics.ContainerMetrics cm : podMetrics.pods().values())
            {
                for (String name : cm.containers().keySet())
                {
                    locationSets.computeIfAbsent(name, k -> new LinkedHashSet<>()).add(location);
                }
            }
        }

        double cpuUsage = totalCpuPerInstant.values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElseThrow(() -> new NoCPUUsageException("Could not compute CPU Usage"));

        double grandTotal = totalCpuPerContainer.values().stream().mapToDouble(Double::doubleValue).sum();
        Map<String, Double> containerShares = grandTotal == 0 ? Map.of()
            : totalCpuPerContainer.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue() / grandTotal
            ));

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
        List<DatapointEntity> datapoints = fetch(timeRange, clusterIds);
        if (datapoints.isEmpty()) throw new NoCPUUsageException("Could not compute CPU Usage");

        Map<Long, Double> totalCpuPerInstant = new HashMap<>();
        for (DatapointEntity dp : datapoints)
        {
            totalCpuPerInstant.merge(dp.instantMs, rowTotalCpu(this.readPayload(dp.payload)), Double::sum);
        }
        return totalCpuPerInstant.values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElseThrow(() -> new NoCPUUsageException("Could not compute CPU Usage"));
    }

    @Override
    public Map<String, Double> getContainerShares(TimeRange timeRange, List<String> clusterIds) throws NoCPUUsageException
    {
        List<DatapointEntity> datapoints = fetch(timeRange, clusterIds);
        if (datapoints.isEmpty()) return Map.of();

        Map<String, Double> totalCpuPerContainer = new HashMap<>();
        for (DatapointEntity dp : datapoints)
        {
            for (PodMetrics.ContainerMetrics cm : this.readPayload(dp.payload).pods().values())
            {
                for (Map.Entry<String, String> c : cm.containers().entrySet())
                {
                    totalCpuPerContainer.merge(c.getKey(), Double.parseDouble(c.getValue()), Double::sum);
                }
            }
        }

        double grandTotal = totalCpuPerContainer.values().stream().mapToDouble(Double::doubleValue).sum();
        if (grandTotal == 0) return Map.of();
        return totalCpuPerContainer.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> e.getValue() / grandTotal
        ));
    }

    @Override
    public Map<String, List<ContainerLocation>> getContainerLocations(TimeRange timeRange, List<String> clusterIds)
    {
        List<DatapointEntity> datapoints = fetch(timeRange, clusterIds);
        if (datapoints.isEmpty()) return Map.of();

        Map<String, Set<ContainerLocation>> locations = new HashMap<>();
        for (DatapointEntity dp : datapoints)
        {
            ContainerLocation location = new ContainerLocation(dp.cluster, dp.namespace);
            for (PodMetrics.ContainerMetrics cm : this.readPayload(dp.payload).pods().values())
            {
                for (String name : cm.containers().keySet())
                {
                    locations.computeIfAbsent(name, k -> new LinkedHashSet<>()).add(location);
                }
            }
        }
        return locations.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> List.copyOf(e.getValue())
        ));
    }

    private List<DatapointEntity> fetch(TimeRange timeRange, List<String> clusterIds)
    {
        return this.repository.findByTypeRangeAndClusters(
            TYPE_KUBERNETES, toEpochMs(timeRange.start()), toEpochMs(timeRange.end()), clusterIds);
    }

    private PodMetrics readPayload(String json)
    {
        try
        {
            return this.mapper.readValue(json, PodMetrics.class);
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to deserialize datapoint payload", e);
        }
    }

    private static double rowTotalCpu(PodMetrics podMetrics)
    {
        return podMetrics.pods().values().stream()
            .flatMap(containers -> containers.containers().values().stream())
            .mapToDouble(Double::parseDouble)
            .sum();
    }

    private static long toEpochMs(Instant instant)
    {
        if (instant.equals(Instant.MIN)) return Long.MIN_VALUE;
        if (instant.equals(Instant.MAX)) return Long.MAX_VALUE;
        return instant.toEpochMilli();
    }
}
