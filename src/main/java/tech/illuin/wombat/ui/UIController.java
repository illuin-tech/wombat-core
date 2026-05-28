package tech.illuin.wombat.ui;

import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import tech.illuin.wombat.handler.ImpactService;
import tech.illuin.wombat.handler.model.BoaviztaKubernetesConfig;
import tech.illuin.wombat.handler.model.ImpactRequest;
import tech.illuin.wombat.handler.model.ImpactResponse;
import tech.illuin.wombat.monitor.MonitorProperties;
import tech.illuin.wombat.persistence.NoCPUUsageException;
import tech.illuin.wombat.persistence.model.TimeRange;
import tech.illuin.wombat.profile.ServerProfileProperties;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Path("ui")
public class UIController
{

    private final ImpactService impactService;
    private final ServerProfileProperties serverProfileProperties;
    private final MonitorProperties monitorProperties;

    public UIController(ImpactService impactService, ServerProfileProperties serverProfileProperties, MonitorProperties monitorProperties)
    {
        this.impactService = impactService;
        this.serverProfileProperties = serverProfileProperties;
        this.monitorProperties = monitorProperties;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get(
        @QueryParam("from") String from,
        @QueryParam("to") String to,
        @QueryParam("containers") List<String> containers
    )
    {
        try
        {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm[:ss]").withZone(ZoneOffset.UTC);

            Instant start = from != null && !from.isBlank()
                ? fmt.parse(from, Instant::from)
                : Instant.now().minus(24, ChronoUnit.HOURS);
            Instant end = to != null && !to.isBlank()
                ? fmt.parse(to, Instant::from)
                : Instant.now().plus(1, ChronoUnit.HOURS);

            ImpactRequest request = new ImpactRequest(
                new TimeRange(start, end),
                List.of(BoaviztaKubernetesConfig.fromServerProfile(this.serverProfileProperties, this.monitorProperties))
            );

            List<ImpactResponse> results = this.impactService.computeImpactResponse(request, containers);
            return Templates.impact(results.getFirst());
        }
        catch (NoCPUUsageException e) {
            return Templates.impactError(from, to);
        }
    }
}
