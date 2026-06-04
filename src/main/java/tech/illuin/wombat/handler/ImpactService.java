package tech.illuin.wombat.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.handler.model.ClusterInfo;
import tech.illuin.wombat.handler.model.ImpactRequest;
import tech.illuin.wombat.handler.model.ImpactResponse;
import tech.illuin.wombat.handler.model.ProviderConfig;
import tech.illuin.wombat.model.Datasource;
import tech.illuin.wombat.model.Footprint;
import tech.illuin.wombat.persistence.LoadData;
import tech.illuin.wombat.persistence.NoCPUUsageException;
import tech.illuin.wombat.persistence.model.TimeRange;

import java.util.*;
import java.util.stream.Collectors;

public class ImpactService
{

    private static final Logger logger = LoggerFactory.getLogger(ImpactService.class);
    private static final String serviceGlobal = "global";

    private final Map<Datasource, ServiceHandler<?>> handlerSelector;

    public ImpactService(Map<Datasource, ServiceHandler<?>> handlerSelector)
    {
        this.handlerSelector = handlerSelector;
    }

    public List<ImpactResponse> computeImpactResponse(ImpactRequest input, List<String> containerIds, List<String> clusterIds) throws NoCPUUsageException
    {
        List<ProviderConfig> configs = input.configs() != null ? input.configs() : List.of();
        List<ImpactResponse> results = new ArrayList<>();
        for (ProviderConfig config : configs)
        {
            ServiceHandler<?> handler = this.handlerSelector.get(config.datasource());
            if (handler == null)
                throw new IllegalArgumentException("No handler registered for datasource: " + config.datasource());
            results.add(computeWithHandler(handler, config, input.sourceTimeRange(), input, containerIds, clusterIds));
        }
        return results;
    }

    public List<ImpactResponse> computeImpactResponse(ImpactRequest input, List<String> containerIds) throws NoCPUUsageException
    {
        return computeImpactResponse(input, containerIds, Collections.emptyList());
    }

    public List<ImpactResponse> computeImpactResponse(ImpactRequest input) throws NoCPUUsageException
    {
        return computeImpactResponse(input, Collections.emptyList(), Collections.emptyList());
    }

    private <R> ImpactResponse computeWithHandler(
        ServiceHandler<R> handler,
        ProviderConfig config,
        TimeRange timeRange,
        ImpactRequest input,
        List<String> containerIds,
        List<String> clusterIds
    ) throws NoCPUUsageException
    {
        LoadData loadData = handler.loadTarget().computeLoad(timeRange, clusterIds);
        double load = loadData.cpuUsage();
        logger.info("Used CPU load {}", load);

        int lifespanHours = config.lifespan();
        R providerResponse = handler.impactProvider().resolveImpact(config, timeRange, load);
        Footprint globalFootprint = handler.footprintResolver().resolveFootprint(providerResponse, serviceGlobal, 1.0d, timeRange, lifespanHours);

        Map<String, Double> containerShares = loadData.containerShares();
        List<String> allContainers = new ArrayList<>(containerShares.keySet());
        Set<String> containerFilter = (containerIds == null || containerIds.isEmpty())
            ? containerShares.keySet()
            : new HashSet<>(containerIds);

        List<Map.Entry<String, Double>> sorted = containerShares.entrySet().stream()
            .filter(e -> containerFilter.contains(e.getKey()))
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .toList();

        List<Footprint> serviceImpact = new LinkedList<>();
        Map<String, Double> filteredShares = new LinkedHashMap<>();
        sorted.forEach(e -> {
            serviceImpact.add(handler.footprintResolver().resolveFootprint(providerResponse, e.getKey(), e.getValue(), timeRange, lifespanHours));
            filteredShares.put(e.getKey(), e.getValue());
        });

        Map<String, List<ClusterInfo>> containerLocations = loadData.containerLocations()
            .entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().stream().map(loc -> new ClusterInfo(loc.clusterId(), loc.namespace())).toList()
            ));

        return new ImpactResponse(globalFootprint, serviceImpact, filteredShares, input, new ImpactResponse.Parameters(config, timeRange), allContainers, containerLocations);
    }
}
