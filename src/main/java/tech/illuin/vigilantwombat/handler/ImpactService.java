package tech.illuin.vigilantwombat.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.vigilantwombat.boavizta.BoaviztaService;
import tech.illuin.vigilantwombat.boavizta.model.BoaviztaInstanceConfigResponse;
import tech.illuin.vigilantwombat.boavizta.model.BoaviztaInstanceImpactRequest;
import tech.illuin.vigilantwombat.boavizta.model.BoaviztaInstanceImpactResponse;
import tech.illuin.vigilantwombat.handler.model.ImpactRequest;
import tech.illuin.vigilantwombat.handler.model.ImpactResponse;
import tech.illuin.vigilantwombat.persistence.LoadTarget;
import tech.illuin.vigilantwombat.persistence.NoCPUUsageException;
import tech.illuin.vigilantwombat.persistence.model.TimeRange;
import tech.illuin.vigilantwombat.profile.ServerConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ImpactService {

    private static final Logger logger = LoggerFactory.getLogger(ImpactService.class);
    private static final String rebalanceWarning = "This usage has been rebalanced by the Vigilant Wombat service, it does not come from the Boavizta API";

    private final BoaviztaService boaviztaService;
    private final LoadTarget loadTarget;

    public ImpactService(BoaviztaService boaviztaService, LoadTarget loadTarget) {
        this.boaviztaService = boaviztaService;
        this.loadTarget = loadTarget;
    }

    public ImpactResponse computeImpactResponse(
        ServerConfig serverConfig,
        TimeRange timeRange,
        ImpactRequest input,
        List<String> containerIds
    ) throws NoCPUUsageException {
        BoaviztaInstanceConfigResponse instanceConfig = this.boaviztaService.getInstanceConfig(serverConfig.provider(), serverConfig.instanceType());
        // TODO: check units
        double loadPercentage = (this.loadTarget.computeCpuUsage(timeRange) / 1000 / 1000 / 1000) / instanceConfig.vcpu().def() * 100;
        logger.info("Current CPU loadPercentage {}", loadPercentage);

        BoaviztaInstanceImpactResponse boaviztaResponse = this.boaviztaService.getInstanceImpact(
            new BoaviztaInstanceImpactRequest(
                serverConfig.provider(),
                serverConfig.instanceType(),
                new BoaviztaInstanceImpactRequest.Usage(serverConfig.location(), List.of(new BoaviztaInstanceImpactRequest.Usage.LoadSegment(100, loadPercentage)))
            ),
            serverConfig.lifespan()
        );

        Map<String, Double> containerShares = this.loadTarget.getContainerShares(timeRange);
        List<String> allContainers = new ArrayList<>(containerShares.keySet());
        Set<String> containerFilter = (containerIds == null || containerIds.isEmpty())
            ? containerShares.keySet()
            : new HashSet<>(containerIds);

        List<Map.Entry<String, Double>> sorted = containerShares.entrySet().stream()
            .filter(e -> containerFilter.contains(e.getKey()))
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .collect(Collectors.toList());

        Map<String, BoaviztaInstanceImpactResponse> serviceImpact = new LinkedHashMap<>();
        Map<String, Double> filteredShares = new LinkedHashMap<>();
        sorted.forEach(e -> {
            serviceImpact.put(e.getKey(), rebalanceImpactResponse(boaviztaResponse, e.getValue()));
            filteredShares.put(e.getKey(), e.getValue());
        });

        return new ImpactResponse(boaviztaResponse, serviceImpact, filteredShares, input, new ImpactResponse.Parameters(serverConfig, timeRange), allContainers);
    }

    public ImpactResponse computeImpactResponse(ServerConfig serverConfig, TimeRange timeRange, ImpactRequest input) throws NoCPUUsageException {
        return this.computeImpactResponse(serverConfig, timeRange, input, Collections.emptyList());
    }

    public ImpactResponse computeImpactResponse(ServerConfig serverConfig, TimeRange timeRange, List<String> containerIds) throws NoCPUUsageException {
        return this.computeImpactResponse(serverConfig, timeRange, new ImpactRequest(serverConfig, timeRange), containerIds);
    }

    private static BoaviztaInstanceImpactResponse rebalanceImpactResponse(BoaviztaInstanceImpactResponse globalImpactResponse, Double share) {
        return new BoaviztaInstanceImpactResponse(
            globalImpactResponse.impacts().entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> {
                        BoaviztaInstanceImpactResponse.Impact globalImpact = e.getValue();
                        BoaviztaInstanceImpactResponse.Impact.ImpactItem globalEmbeddedImpact = globalImpact.embedded();
                        BoaviztaInstanceImpactResponse.Impact.ImpactItem globalUseImpact = globalImpact.use();

                        List<String> embeddedWarnings = new ArrayList<>(globalEmbeddedImpact.warnings() == null ? Collections.emptyList() : globalEmbeddedImpact.warnings());
                        embeddedWarnings.add(rebalanceWarning);
                        List<String> useWarnings = new ArrayList<>(globalUseImpact.warnings() == null ? Collections.emptyList() : globalUseImpact.warnings());
                        useWarnings.add(rebalanceWarning);

                        return new BoaviztaInstanceImpactResponse.Impact(
                            globalImpact.unit(),
                            globalImpact.description(),
                            new BoaviztaInstanceImpactResponse.Impact.ImpactItem(
                                (float) (globalEmbeddedImpact.value() * share),
                                (float) (globalEmbeddedImpact.min() * share),
                                (float) (globalEmbeddedImpact.max() * share),
                                embeddedWarnings
                            ),
                            new BoaviztaInstanceImpactResponse.Impact.ImpactItem(
                                (float) (globalUseImpact.value() * share),
                                (float) (globalUseImpact.min() * share),
                                (float) (globalUseImpact.max() * share),
                                useWarnings
                            )
                        );
                    })
                )
        );
    }
}
