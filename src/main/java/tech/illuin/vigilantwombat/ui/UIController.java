package tech.illuin.vigilantwombat.ui;

import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import tech.illuin.vigilantwombat.handler.ImpactService;
import tech.illuin.vigilantwombat.handler.model.ImpactResponse;
import tech.illuin.vigilantwombat.persistence.NoCPUUsageException;
import tech.illuin.vigilantwombat.persistence.model.TimeRange;
import tech.illuin.vigilantwombat.profile.ServerConfig;
import tech.illuin.vigilantwombat.profile.ServerProfileProperties;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Path("ui")
public class UIController {

    private final ImpactService impactService;
    private final ServerProfileProperties serverProfileProperties;

    public UIController(ImpactService impactService, ServerProfileProperties serverProfileProperties) {
        this.impactService = impactService;
        this.serverProfileProperties = serverProfileProperties;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get(
        @QueryParam("from") String from,
        @QueryParam("to") String to,
        @QueryParam("containers") List<String> containers
    ) {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm[:ss]").withZone(ZoneOffset.UTC);

            Instant start = from != null && !from.isBlank()
                ? fmt.parse(from, Instant::from)
                : Instant.now().minus(24, ChronoUnit.HOURS);
            Instant end = to != null && !to.isBlank()
                ? fmt.parse(to, Instant::from)
                : Instant.now();


            ServerConfig serverConfig = ServerConfig.fromServerProfile(this.serverProfileProperties);
            TimeRange timeRange = new TimeRange(start, end);

            ImpactResponse result = impactService.computeImpactResponse(serverConfig, timeRange, containers);
            return Templates.impact(result);
        }
        catch (NoCPUUsageException e) {
            return Templates.impactError(from, to);
        }
    }
}
