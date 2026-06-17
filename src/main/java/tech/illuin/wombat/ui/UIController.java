package tech.illuin.wombat.ui;

import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import tech.illuin.wombat.asset.AssetConfig;
import tech.illuin.wombat.asset.AssetService;
import tech.illuin.wombat.boavizta.BoaviztaClient;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceConfigResponse;
import tech.illuin.wombat.handler.ImpactService;
import tech.illuin.wombat.handler.model.ImpactRequest;
import tech.illuin.wombat.handler.model.ImpactResponse;
import tech.illuin.wombat.persistence.NoCPUUsageException;
import tech.illuin.wombat.persistence.model.TimeRange;
import tech.illuin.wombat.profile.Profile;

import java.time.Duration;
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
    private final AssetService assetService;
    private final BoaviztaClient boaviztaClient;
    private final UIProperties uiProperties;

    public UIController(ImpactService impactService, AssetService assetService, @RestClient BoaviztaClient boaviztaClient, UIProperties uiProperties)
    {
        this.impactService = impactService;
        this.assetService = assetService;
        this.boaviztaClient = boaviztaClient;
        this.uiProperties = uiProperties;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get(
        @QueryParam("from") String from,
        @QueryParam("to") String to,
        @QueryParam("containers") List<String> containers,
        @QueryParam("assets") List<String> assets
    )
    {
        try
        {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm[:ss]").withZone(ZoneOffset.UTC);

            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
            Duration maxSpan = this.uiProperties.maxDateRange().asDuration();

            Instant end = to != null && !to.isBlank()
                ? fmt.parse(to, Instant::from)
                : now.toInstant();
            Instant start = from != null && !from.isBlank()
                ? fmt.parse(from, Instant::from)
                : ZonedDateTime.ofInstant(end, ZoneOffset.UTC).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant();
            if (start.isAfter(end)) start = end;
            if (Duration.between(start, end).compareTo(maxSpan) > 0) start = end.minus(maxSpan);

            List<AssetConfig> allAssets = this.assetService.all();
            if (allAssets.isEmpty())
                throw new IllegalStateException("No assets configured (cluster.profile-id missing or referenced profile not found)");

            List<String> effectiveAssetIds = (assets == null || assets.isEmpty())
                ? List.of(allAssets.getFirst().clusterProperties().id())
                : assets;
            List<AssetConfig> selectedAssets = this.assetService.resolve(effectiveAssetIds);
            if (selectedAssets.isEmpty()) selectedAssets = List.of(allAssets.getFirst());

            ImpactRequest request = new ImpactRequest(
                new TimeRange(start, end),
                selectedAssets.stream().map(a -> a.clusterProperties().id()).toList()
            );

            List<ImpactResponse> results = this.impactService.computeImpactResponse(request, containers, selectedAssets);
            AssetConfig selected = selectedAssets.getFirst();
            Profile profile = selected.profile();
            BoaviztaInstanceConfigResponse instanceConfig = this.boaviztaClient.getInstanceConfig(profile.provider(), profile.instanceType());
            ImpactResponse first = results.getFirst();
            int vcpu = instanceConfig.vcpu() != null && instanceConfig.vcpu().def() != null ? instanceConfig.vcpu().def() : 0;
            double loadPercent = vcpu > 0 ? first.cpuUsageCores() / vcpu * 100.0 : 0.0;
            String maxSpanLabel = this.uiProperties.maxDateRange().duration() + " " + this.uiProperties.maxDateRange().unit().name().toLowerCase();
            Templates.MaxSpan span = new Templates.MaxSpan(maxSpan.toMillis(), maxSpanLabel);
            return Templates.impact(first, allAssets, effectiveAssetIds, selected, instanceConfig, loadPercent, span);
        }
        catch (NoCPUUsageException e) {
            return Templates.impactError(from, to);
        }
    }
}
