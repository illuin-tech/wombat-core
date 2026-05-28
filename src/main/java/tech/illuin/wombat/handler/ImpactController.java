package tech.illuin.wombat.handler;

import tech.illuin.wombat.handler.model.BoaviztaKubernetesConfig;
import tech.illuin.wombat.handler.model.ImpactRequest;
import tech.illuin.wombat.handler.model.ImpactResponse;
import tech.illuin.wombat.handler.model.ProviderConfig;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import tech.illuin.wombat.monitor.MonitorProperties;
import tech.illuin.wombat.persistence.NoCPUUsageException;
import tech.illuin.wombat.persistence.model.TimeRange;
import tech.illuin.wombat.profile.ServerProfileProperties;
import tech.illuin.wombat.response.Response;

import java.time.Instant;
import java.util.List;

@Path("impact")
public class ImpactController {

    private static final TimeRange defaultTimeRange = new TimeRange(Instant.MIN, Instant.MAX);

    private final ServerProfileProperties serverProfileProperties;
    private final MonitorProperties monitorProperties;
    private final ImpactService impactService;

    public ImpactController(ServerProfileProperties serverProfileProperties, MonitorProperties monitorProperties, ImpactService impactService) {
        this.serverProfileProperties = serverProfileProperties;
        this.monitorProperties = monitorProperties;
        this.impactService = impactService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response<List<ImpactResponse>> getImpact(ImpactRequest input) {
        try {
            List<ProviderConfig> configs = resolveConfigs(input);
            TimeRange timeRange = input == null || input.sourceTimeRange() == null ? defaultTimeRange : input.sourceTimeRange();
            List<ImpactResponse> response = this.impactService.computeImpactResponse(new ImpactRequest(timeRange, configs));
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

    private List<ProviderConfig> resolveConfigs(ImpactRequest input) {
        if (input != null && input.configs() != null && !input.configs().isEmpty())
            return input.configs();
        if (!this.serverProfileProperties.enable())
            throw new WebApplicationException(
                jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.BAD_REQUEST)
                    .entity("Required provider config not provided")
                    .build()
            );
        return List.of(BoaviztaKubernetesConfig.fromServerProfile(this.serverProfileProperties, this.monitorProperties));
    }
}
