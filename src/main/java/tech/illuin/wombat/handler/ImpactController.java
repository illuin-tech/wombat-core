package tech.illuin.wombat.handler;

import tech.illuin.wombat.handler.model.BoaviztaKubernetesConfig;
import tech.illuin.wombat.handler.model.ClusterInfo;
import tech.illuin.wombat.handler.model.ImpactRequest;
import tech.illuin.wombat.handler.model.ImpactResponse;
import tech.illuin.wombat.handler.model.ProviderConfig;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import tech.illuin.wombat.monitor.MonitorProperties;
import tech.illuin.wombat.persistence.NoCPUUsageException;
import tech.illuin.wombat.persistence.model.TimeRange;
import tech.illuin.wombat.profile.ProfileSeedProperties;
import tech.illuin.wombat.profile.ServerProfileEntity;
import tech.illuin.wombat.profile.ServerProfileRepository;
import tech.illuin.wombat.response.Response;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Path("impact")
public class ImpactController
{

    private static TimeRange currentMonthTimeRange()
    {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        Instant start = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant();
        Instant end = now.plusMonths(1).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant();
        return new TimeRange(start, end);
    }

    private final ServerProfileRepository profileRepository;
    private final ProfileSeedProperties seedProperties;
    private final MonitorProperties monitorProperties;
    private final ImpactService impactService;

    public ImpactController(ServerProfileRepository profileRepository, ProfileSeedProperties seedProperties, MonitorProperties monitorProperties, ImpactService impactService)
    {
        this.profileRepository = profileRepository;
        this.seedProperties = seedProperties;
        this.monitorProperties = monitorProperties;
        this.impactService = impactService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response<List<ImpactResponse>> getImpact(ImpactRequest input)
    {
        try
        {
            List<ProviderConfig> configs = resolveConfigs(input);
            TimeRange timeRange = input == null || input.sourceTimeRange() == null ? currentMonthTimeRange() : input.sourceTimeRange();
            List<String> clusterIds = configs.stream()
                .filter(BoaviztaKubernetesConfig.class::isInstance)
                .map(BoaviztaKubernetesConfig.class::cast)
                .flatMap(c -> c.clusters().stream())
                .map(ClusterInfo::id)
                .distinct()
                .toList();
            List<ImpactResponse> response = this.impactService.computeImpactResponse(new ImpactRequest(timeRange, configs), List.of(), clusterIds);
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

    private List<ProviderConfig> resolveConfigs(ImpactRequest input)
    {
        if (input != null && input.configs() != null && !input.configs().isEmpty())
            return input.configs();
        ServerProfileEntity profile = this.profileRepository.findByIdOptional(this.seedProperties.defaultId())
            .orElseGet(() -> this.profileRepository.listAll().stream().findFirst()
                .orElseThrow(() -> new WebApplicationException(
                    jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.BAD_REQUEST)
                        .entity("No server profile configured")
                        .build()
                )));
        return List.of(BoaviztaKubernetesConfig.fromProfileEntity(profile, this.monitorProperties, List.of()));
    }
}
