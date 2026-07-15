package tech.illuin.wombat.handler;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import tech.illuin.wombat.asset.AssetImpactService;
import tech.illuin.wombat.asset.AssetService;
import tech.illuin.wombat.asset.UnknownEnvironmentException;
import tech.illuin.wombat.handler.model.EnvironmentConfig;
import tech.illuin.wombat.handler.model.GlobalImpact;
import tech.illuin.wombat.handler.model.ImpactRequest;
import tech.illuin.wombat.handler.model.ImpactResponse;
import tech.illuin.wombat.model.Footprint;
import tech.illuin.wombat.monitor.Environment;
import tech.illuin.wombat.persistence.NoCPUUsageException;
import tech.illuin.wombat.persistence.model.TimeRange;
import tech.illuin.wombat.response.Response;
import tech.illuin.wombat.ui.AssetImpact;
import tech.illuin.wombat.ui.KubernetesAssetImpact;
import tech.illuin.wombat.ui.LLMAssetImpact;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Path("impact")
public class ImpactController
{
    private final AssetService assetService;
    private final AssetImpactService assetImpactService;

    public ImpactController(AssetService assetService, AssetImpactService assetImpactService)
    {
        this.assetService = assetService;
        this.assetImpactService = assetImpactService;
    }

    @GET
    @Path("environments")
    @Produces(MediaType.APPLICATION_JSON)
    public Response<List<EnvironmentConfig>> getEnvironments()
    {
        List<EnvironmentConfig> environments = this.assetService.environments().entrySet().stream()
            .map(entry -> EnvironmentConfig.from(entry.getKey(), entry.getValue()))
            .toList();
        return Response.success(environments);
    }

    @GET
    @Path("environments/{environment}/assets")
    @Produces(MediaType.APPLICATION_JSON)
    public Response<List<EnvironmentConfig.AssetSummary>> getAssets(@PathParam("environment") String environment)
    {
        Environment properties = this.assetService.getEnvironmentProperties(environment);
        if (properties == null)
            throw new UnknownEnvironmentException(environment);
        List<EnvironmentConfig.AssetSummary> assets = properties.assets().stream()
            .map(EnvironmentConfig.AssetSummary::from)
            .toList();
        return Response.success(assets);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response<ImpactResponse> getImpact(ImpactRequest input) throws NoCPUUsageException
    {
        TimeRange timeRange = input == null || input.sourceTimeRange() == null
            ? currentMonthTimeRange()
            : input.sourceTimeRange();
        String environmentId = input == null ? null : input.environmentId();
        List<String> assetIds = input == null || input.assetIds() == null ? List.of() : input.assetIds();

        Map<String, AssetImpact> assetImpacts = this.assetImpactService.computeImpacts(environmentId, assetIds, timeRange);
        List<Footprint> footprints = assetImpacts.values().stream().map(ImpactController::globalFootprint).toList();

        return Response.success(new ImpactResponse(
            GlobalImpact.from(footprints),
            assetImpacts,
            this.resolveEnvironmentConfig(environmentId, timeRange)
        ));
    }

    private EnvironmentConfig resolveEnvironmentConfig(String environmentId, TimeRange timeRange)
    {
        if (environmentId == null || environmentId.isBlank())
            return null;
        Environment environment = this.assetService.getEnvironmentProperties(environmentId);
        return environment == null ? null : EnvironmentConfig.from(environmentId, environment, timeRange);
    }

    private static Footprint globalFootprint(AssetImpact impact)
    {
        return switch (impact)
        {
            case KubernetesAssetImpact kubernetes -> kubernetes.response().globalImpact();
            case LLMAssetImpact llm -> llm.toFootprint();
            default -> throw new IllegalStateException("Unsupported asset impact type: " + impact.type());
        };
    }

    private static TimeRange currentMonthTimeRange()
    {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        Instant start = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant();
        Instant end = now.plusMonths(1).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant();
        return new TimeRange(start, end);
    }
}
