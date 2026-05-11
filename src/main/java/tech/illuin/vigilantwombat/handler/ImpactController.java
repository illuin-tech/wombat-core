package tech.illuin.vigilantwombat.handler;

import tech.illuin.vigilantwombat.boavizta.BoaviztaService;
import tech.illuin.vigilantwombat.boavizta.model.BoaviztaInstanceConfigResponse;
import tech.illuin.vigilantwombat.boavizta.model.BoaviztaInstanceImpactRequest;
import tech.illuin.vigilantwombat.boavizta.model.BoaviztaInstanceImpactResponse;
import tech.illuin.vigilantwombat.handler.model.ImpactRequest;
import tech.illuin.vigilantwombat.handler.model.ImpactResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.vigilantwombat.persistence.MemoryLoadTarget;
import tech.illuin.vigilantwombat.persistence.NoCPUUsageException;
import tech.illuin.vigilantwombat.persistence.model.TimeRange;
import tech.illuin.vigilantwombat.profile.ServerConfig;
import tech.illuin.vigilantwombat.profile.ServerProfileProperties;
import tech.illuin.vigilantwombat.response.Response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("impact")
public class ImpactController {

    private static final Logger logger = LoggerFactory.getLogger(ImpactController.class);
    private static final String rebalanceWarning = "This usage has been rebalanced by the Vigilant Wombat service, it does not come from the Boavizta API";
    private static final TimeRange defaultTimeRange = new TimeRange(Instant.MIN, Instant.MAX);

    @Inject BoaviztaService boaviztaService;
    private final MemoryLoadTarget memoryLoadTarget;
    private final ServerProfileProperties serverProfileProperties;

    public ImpactController(MemoryLoadTarget memoryLoadTarget, ServerProfileProperties serverProfileProperties) {
        this.memoryLoadTarget = memoryLoadTarget;
        this.serverProfileProperties = serverProfileProperties;
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response<ImpactResponse> getImpact(ImpactRequest input)
    {
        try {
            if ((input == null || input.serverConfig() == null) && !this.serverProfileProperties.enable())
                throw new WebApplicationException(
                    jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.BAD_REQUEST)
                        .entity("Required server config not provided")
                        .build()
                );
            ServerConfig serverConfig = (input != null && input.serverConfig() != null) ? input.serverConfig() : ServerConfig.fromServerProfile(this.serverProfileProperties);
            TimeRange timeRange = input == null || input.sourceTimeRange() == null ? defaultTimeRange : input.sourceTimeRange();

            BoaviztaInstanceConfigResponse instanceConfig = this.boaviztaService.getInstanceConfig(serverConfig.provider(), serverConfig.instanceType());
            // TODO: check units
            double loadPercentage = (this.memoryLoadTarget.computeCpuUsage(timeRange) / 1000 / 1000 / 1000) / instanceConfig.vcpu().def() * 100;
            logger.info("Current memory loadPercentage {}", loadPercentage);

            BoaviztaInstanceImpactResponse boaviztaResponse = this.boaviztaService.getInstanceImpact(
                new BoaviztaInstanceImpactRequest(
                    serverConfig.provider(),
                    serverConfig.instanceType(),
                    new BoaviztaInstanceImpactRequest.Usage(serverConfig.location(), List.of(new BoaviztaInstanceImpactRequest.Usage.LoadSegment(100, loadPercentage)))
                ),
                serverConfig.lifespan()
            );

            Map<String, Double> containerShares = this.memoryLoadTarget.getContainerShares(timeRange);
            ImpactResponse response = new ImpactResponse(
                boaviztaResponse,
                containerShares.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> rebalanceImpactResponse(boaviztaResponse, e.getValue()))),
                containerShares,
                input,
                new ImpactResponse.Parameters(serverConfig, timeRange)
            );

            return Response.success(response);
        }
        catch (NoCPUUsageException e) {
            throw new WebApplicationException(
                jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.BAD_REQUEST)
                    .entity("No CPU Usage Could be found")
                    .build()
            );
        }
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
                                Double.valueOf(globalEmbeddedImpact.value() * share).intValue(),
                                Double.valueOf(globalEmbeddedImpact.min() * share).intValue(),
                                Double.valueOf(globalEmbeddedImpact.max() * share).intValue(),
                                embeddedWarnings
                            ),
                            new BoaviztaInstanceImpactResponse.Impact.ImpactItem(
                                Double.valueOf(globalUseImpact.value() * share).intValue(),
                                Double.valueOf(globalUseImpact.min() * share).intValue(),
                                Double.valueOf(globalUseImpact.max() * share).intValue(),
                                useWarnings
                            )
                        );
                    })
                )
        );
    }
}
