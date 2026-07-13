package tech.illuin.wombat.ui;

import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import tech.illuin.wombat.asset.model.Asset;
import tech.illuin.wombat.asset.AssetImpactService;
import tech.illuin.wombat.asset.AssetService;
import tech.illuin.wombat.persistence.NoCPUUsageException;
import tech.illuin.wombat.persistence.model.TimeRange;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("ui")
public class UIController
{
    private final AssetService assetService;
    private final AssetImpactService assetImpactService;
    private final UIProperties uiProperties;

    public UIController(AssetService assetService, AssetImpactService assetImpactService, UIProperties uiProperties)
    {
        this.assetService = assetService;
        this.assetImpactService = assetImpactService;
        this.uiProperties = uiProperties;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get(
        @QueryParam("from") String from,
        @QueryParam("to") String to,
        @QueryParam("services") List<String> servicesParam,
        @QueryParam("environment") String environment
    )
    {
        try
        {
            TimeRange timeRange = this.computeTimeRange(from, to);

            List<Asset> allAssets = this.assetService.all();
            if (allAssets.isEmpty())
                throw new IllegalStateException("No assets configured (cluster.profile-id missing or referenced profile not found)");

            LinkedHashMap<String, String> environmentNames = new LinkedHashMap<>();
            allAssets.forEach(a -> environmentNames.putIfAbsent(a.environmentId(), a.environmentName()));
            List<Templates.EnvironmentView> environments = environmentNames.entrySet().stream()
                .map(e -> new Templates.EnvironmentView(e.getKey(), e.getValue()))
                .toList();

            String selectedEnvironmentId = environment != null && environmentNames.containsKey(environment)
                ? environment
                : environments.getFirst().id();

            List<Asset> environmentAssets = allAssets.stream()
                .filter(a -> a.environmentId().equals(selectedEnvironmentId))
                .toList();

            Map<String, List<String>> services = parseServices(servicesParam);

            List<String> effectiveAssetIds = services.keySet().stream()
                .filter(id -> environmentAssets.stream().anyMatch(a -> a.properties().id().equals(id)))
                .toList();
            if (effectiveAssetIds.isEmpty())
                effectiveAssetIds = environmentAssets.stream().map(a -> a.properties().id()).toList();

            List<String> selectedIds = effectiveAssetIds;
            List<Asset> selectedAssets = environmentAssets.stream()
                .filter(a -> selectedIds.contains(a.properties().id()))
                .toList();

            Map<String, AssetImpact> assetImpacts = this.assetImpactService.computeImpacts(selectedAssets, services, timeRange);

            EnvironmentImpact environmentImpact = EnvironmentImpact.from(List.copyOf(assetImpacts.values()), timeRange);
            String maxSpanLabel = this.uiProperties.maxDateRange().duration() + " " + this.uiProperties.maxDateRange().unit().name().toLowerCase();
            Templates.MaxSpan span = new Templates.MaxSpan(this.uiProperties.maxDateRange().asDuration().toMillis(), maxSpanLabel);
            Templates.AssetSelection assetSelection = new Templates.AssetSelection(environmentAssets, effectiveAssetIds);
            Templates.EnvironmentSelection environmentSelection = new Templates.EnvironmentSelection(environments, selectedEnvironmentId);
            return Templates.impact(environmentImpact, assetSelection, environmentSelection, span);
        }
        catch (NoCPUUsageException e) {
            return Templates.impactError(from, to);
        }
    }

    private TimeRange computeTimeRange(String from, String to)
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
        return new TimeRange(start, end);
    }

    private static Map<String, List<String>> parseServices(List<String> entries)
    {
        Map<String, List<String>> services = new LinkedHashMap<>();
        if (entries == null) return services;
        for (String entry : entries)
        {
            if (entry == null || entry.isBlank()) continue;
            int separator = entry.indexOf('=');
            String assetId = (separator < 0 ? entry : entry.substring(0, separator)).trim();
            if (assetId.isEmpty()) continue;
            List<String> assetServices = separator < 0
                ? List.of()
                : Arrays.stream(entry.substring(separator + 1).split(","))
                    .map(String::trim)
                    .filter(service -> !service.isEmpty())
                    .toList();
            services.put(assetId, assetServices);
        }
        return services;
    }
}
