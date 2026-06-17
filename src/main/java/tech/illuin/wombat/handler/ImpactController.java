package tech.illuin.wombat.handler;

import tech.illuin.wombat.asset.AssetConfig;
import tech.illuin.wombat.asset.AssetService;
import tech.illuin.wombat.handler.model.ImpactRequest;
import tech.illuin.wombat.handler.model.ImpactResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import tech.illuin.wombat.persistence.NoCPUUsageException;
import tech.illuin.wombat.persistence.model.TimeRange;
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

    private final AssetService assetService;
    private final ImpactService impactService;

    public ImpactController(AssetService assetService, ImpactService impactService)
    {
        this.assetService = assetService;
        this.impactService = impactService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response<List<ImpactResponse>> getImpact(ImpactRequest input)
    {
        try
        {
            TimeRange timeRange = input == null || input.sourceTimeRange() == null
                ? currentMonthTimeRange()
                : input.sourceTimeRange();
            List<String> assetIds = input == null ? List.of() : input.assetIds();
            List<AssetConfig> assets = this.assetService.resolve(assetIds == null ? List.of() : assetIds);
            if (assets.isEmpty())
                throw new WebApplicationException(
                    jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.BAD_REQUEST)
                        .entity("No assets matched the requested ids, and no clusters are configured")
                        .build()
                );
            ImpactRequest normalized = new ImpactRequest(timeRange, assets.stream().map(a -> a.clusterProperties().id()).toList());
            List<ImpactResponse> response = this.impactService.computeImpactResponse(normalized, List.of(), assets);
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
}
