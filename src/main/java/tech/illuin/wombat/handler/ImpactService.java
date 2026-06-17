package tech.illuin.wombat.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.asset.AssetConfig;
import tech.illuin.wombat.handler.model.BoaviztaKubernetesConfig;
import tech.illuin.wombat.handler.model.ClusterInfo;
import tech.illuin.wombat.handler.model.ImpactRequest;
import tech.illuin.wombat.handler.model.ImpactResponse;
import tech.illuin.wombat.handler.model.ProviderConfig;
import tech.illuin.wombat.model.Datasource;
import tech.illuin.wombat.model.Footprint;
import tech.illuin.wombat.persistence.LoadData;
import tech.illuin.wombat.persistence.NoCPUUsageException;
import tech.illuin.wombat.persistence.model.TimeRange;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    public List<ImpactResponse> computeImpactResponse(
        ImpactRequest input,
        List<String> containerIds,
        List<AssetConfig> assets
    ) throws NoCPUUsageException
    {
        List<ImpactResponse> results = new ArrayList<>();
        for (AssetConfig asset : assets)
        {
            BoaviztaKubernetesConfig config = BoaviztaKubernetesConfig.fromAsset(asset);
            ServiceHandler<?> handler = this.handlerSelector.get(config.datasource());
            if (handler == null)
                throw new IllegalArgumentException("No handler registered for datasource: " + config.datasource());
            List<String> clusterIds = List.of(asset.clusterProperties().id());
            results.add(computeWithHandler(handler, config, input.sourceTimeRange(), input, containerIds, clusterIds));
        }
        return results;
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
        logger.info("Used CPU load {}", load / 1000 / 1000 / 1000);

        int lifespanHours = config.lifespan();
        R providerResponse = handler.impactProvider().resolveImpact(config, timeRange, load);

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

        double globalShare = filteredShares.values().stream().mapToDouble(Double::doubleValue).sum();
        Footprint globalFootprint = handler.footprintResolver().resolveFootprint(providerResponse, serviceGlobal, globalShare, timeRange, lifespanHours);

        Map<String, List<ClusterInfo>> containerLocations = loadData.containerLocations()
            .entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().stream().map(loc -> new ClusterInfo(loc.clusterId(), loc.namespace())).toList()
            ));

        double cpuUsageCores = load / 1_000_000_000.0;
        return new ImpactResponse(globalFootprint, serviceImpact, filteredShares, input, new ImpactResponse.Parameters(config, timeRange), allContainers, containerLocations, cpuUsageCores);
    }
}
