package tech.illuin.wombat.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.asset.model.KubernetesAsset;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactResponse;
import tech.illuin.wombat.handler.impl.footprint_resolver.BoaviztaFootprintResolver;
import tech.illuin.wombat.handler.impl.impact_provider.BoaviztaImpactProvider;
import tech.illuin.wombat.handler.model.BoaviztaKubernetesConfig;
import tech.illuin.wombat.handler.model.ClusterInfo;
import tech.illuin.wombat.handler.model.KubernetesImpactResponse;
import tech.illuin.wombat.model.Footprint;
import tech.illuin.wombat.persistence.KubernetesMetricsPersister;
import tech.illuin.wombat.persistence.LoadData;
import tech.illuin.wombat.persistence.NoCPUUsageException;
import tech.illuin.wombat.persistence.model.TimeRange;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class KubernetesImpactService
{
    private static final Logger logger = LoggerFactory.getLogger(KubernetesImpactService.class);
    private static final String SERVICE_GLOBAL = "global";
    private static final double NANOCORES_PER_CORE = 1_000_000_000.0;

    private final KubernetesMetricsPersister metricsPersister;
    private final BoaviztaImpactProvider impactProvider;
    private final BoaviztaFootprintResolver footprintResolver;

    public KubernetesImpactService(
        KubernetesMetricsPersister metricsPersister,
        BoaviztaImpactProvider impactProvider,
        BoaviztaFootprintResolver footprintResolver
    )
    {
        this.metricsPersister = metricsPersister;
        this.impactProvider = impactProvider;
        this.footprintResolver = footprintResolver;
    }

    public List<KubernetesImpactResponse> computeImpactResponse(
        TimeRange timeRange,
        Collection<String> serviceIds,
        Collection<KubernetesAsset> assets
    ) throws NoCPUUsageException
    {
        List<KubernetesImpactResponse> results = new ArrayList<>();
        for (KubernetesAsset asset : assets)
        {
            BoaviztaKubernetesConfig config = BoaviztaKubernetesConfig.fromAsset(asset);
            results.add(this.compute(config, timeRange, serviceIds, List.of(asset.properties().id())));
        }
        return results;
    }

    private KubernetesImpactResponse compute(
        BoaviztaKubernetesConfig config,
        TimeRange timeRange,
        Collection<String> serviceIds,
        Collection<String> clusterIds
    ) throws NoCPUUsageException
    {
        LoadData loadData = this.metricsPersister.computeLoad(timeRange, clusterIds);
        logger.info("Used CPU load {}", loadData.cpuUsage() / NANOCORES_PER_CORE);

        BoaviztaInstanceImpactResponse providerResponse = this.impactProvider.resolveImpact(config, timeRange, loadData.cpuUsage());

        List<Map.Entry<String, Double>> includedShares = includedShares(loadData.containerShares(), serviceIds);
        List<Footprint> serviceImpacts = this.resolveServiceFootprints(providerResponse, includedShares, timeRange, config.lifespan());
        Footprint globalFootprint = this.resolveGlobalFootprint(providerResponse, includedShares, timeRange, config.lifespan());

        return new KubernetesImpactResponse(
            globalFootprint,
            serviceImpacts,
            gwpShares(serviceImpacts),
            config,
            new ArrayList<>(loadData.containerShares().keySet()),
            serviceLocations(loadData),
            loadData.cpuUsage() / NANOCORES_PER_CORE
        );
    }

    private List<Footprint> resolveServiceFootprints(
        BoaviztaInstanceImpactResponse providerResponse,
        List<Map.Entry<String, Double>> shares,
        TimeRange timeRange,
        int lifespanHours
    )
    {
        List<Footprint> serviceImpacts = new ArrayList<>();
        shares.forEach(share ->
            serviceImpacts.add(this.footprintResolver.resolveFootprint(providerResponse, share.getKey(), share.getValue(), timeRange, lifespanHours))
        );
        serviceImpacts.sort(Comparator.comparingDouble((Footprint f) -> f.gwp().totalValue()).reversed());
        return serviceImpacts;
    }

    private Footprint resolveGlobalFootprint(
        BoaviztaInstanceImpactResponse providerResponse,
        List<Map.Entry<String, Double>> shares,
        TimeRange timeRange,
        int lifespanHours
    )
    {
        double globalShare = shares.stream().mapToDouble(Map.Entry::getValue).sum();
        return this.footprintResolver.resolveFootprint(providerResponse, SERVICE_GLOBAL, globalShare, timeRange, lifespanHours);
    }

    private static List<Map.Entry<String, Double>> includedShares(Map<String, Double> containerShares, Collection<String> serviceIds)
    {
        Set<String> serviceFilter = (serviceIds == null || serviceIds.isEmpty())
            ? containerShares.keySet()
            : new HashSet<>(serviceIds);
        return containerShares.entrySet().stream()
            .filter(share -> serviceFilter.contains(share.getKey()))
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .toList();
    }

    private static Map<String, Double> gwpShares(List<Footprint> serviceImpacts)
    {
        double totalGwp = serviceImpacts.stream().mapToDouble(f -> f.gwp().totalValue()).sum();
        Map<String, Double> shares = new LinkedHashMap<>();
        serviceImpacts.forEach(footprint ->
            shares.put(footprint.service(), totalGwp == 0.0 ? 0.0 : footprint.gwp().totalValue() / totalGwp)
        );
        return shares;
    }

    private static Map<String, List<ClusterInfo>> serviceLocations(LoadData loadData)
    {
        return loadData.containerLocations()
            .entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().stream().map(loc -> new ClusterInfo(loc.clusterId(), loc.namespace())).toList()
            ));
    }
}
