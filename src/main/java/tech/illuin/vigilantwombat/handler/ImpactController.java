package tech.illuin.vigilantwombat.handler;

import tech.illuin.vigilantwombat.handler.model.ImpactRequest;
import tech.illuin.vigilantwombat.handler.model.ImpactResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import tech.illuin.vigilantwombat.persistence.NoCPUUsageException;
import tech.illuin.vigilantwombat.persistence.model.TimeRange;
import tech.illuin.vigilantwombat.profile.ServerConfig;
import tech.illuin.vigilantwombat.profile.ServerProfileProperties;
import tech.illuin.vigilantwombat.response.Response;

import java.time.Instant;

@Path("impact")
public class ImpactController {

    private static final TimeRange defaultTimeRange = new TimeRange(Instant.MIN, Instant.MAX);

    private final ServerProfileProperties serverProfileProperties;
    private final ImpactService impactService;

    public ImpactController(ServerProfileProperties serverProfileProperties, ImpactService impactService) {
        this.serverProfileProperties = serverProfileProperties;
        this.impactService = impactService;
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
            ImpactResponse response = this.impactService.computeImpactResponse(serverConfig, timeRange, input);

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
