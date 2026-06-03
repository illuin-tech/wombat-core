package tech.illuin.wombat.ui;

import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import tech.illuin.wombat.handler.ImpactService;
import tech.illuin.wombat.handler.model.BoaviztaKubernetesConfig;
import tech.illuin.wombat.handler.model.ClusterInfo;
import tech.illuin.wombat.handler.model.ImpactRequest;
import tech.illuin.wombat.handler.model.ImpactResponse;
import tech.illuin.wombat.monitor.MonitorProperties;
import tech.illuin.wombat.persistence.NoCPUUsageException;
import tech.illuin.wombat.persistence.model.TimeRange;
import tech.illuin.wombat.profile.ServerProfileProperties;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
        @QueryParam("containers") List<String> containers,
        @QueryParam("clusters") List<String> clusters
    )
    {
        try
        {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm[:ss]").withZone(ZoneOffset.UTC);

            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
            Instant start = from != null && !from.isBlank()
                ? fmt.parse(from, Instant::from)
                : now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant();
            Instant end = to != null && !to.isBlank()
                ? fmt.parse(to, Instant::from)
                : now.plusMonths(1).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant();

            List<ClusterInfo> allClusters = this.monitorProperties.k8sConfigs().clusters().stream()
                .map(c -> new ClusterInfo(c.id(), c.namespace()))
                .toList();

            List<String> effectiveClusters = (clusters == null || clusters.isEmpty())
                ? allClusters.stream().findFirst().map(c -> List.of(c.id())).orElse(List.of())
                : clusters;

            ImpactRequest request = new ImpactRequest(
                new TimeRange(start, end),
                List.of(BoaviztaKubernetesConfig.fromServerProfile(this.serverProfileProperties, this.monitorProperties, effectiveClusters))
            );

            List<ImpactResponse> results = this.impactService.computeImpactResponse(request, containers, effectiveClusters);
            return Templates.impact(results.getFirst(), allClusters, effectiveClusters);
        }
        catch (NoCPUUsageException e) {
            return Templates.impactError(from, to);
        }
    }
}
