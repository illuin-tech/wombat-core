package tech.illuin.wombat.module.llm_vertexai.activity;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.core.activity.WombatActivityException;
import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.activity.commons.*;
import tech.illuin.wombat.core.activity.llm.LLMActivityData;
import tech.illuin.wombat.core.activity.llm.LLMServiceActivity;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.module.llm_vertexai.LLMVertexAIAsset;
import tech.illuin.wombat.module.llm_vertexai.LLMVertexAIProfile;
import tech.illuin.wombat.module.llm_vertexai.connector.GoogleClient;
import tech.illuin.wombat.module.llm_vertexai.connector.GoogleMultiClient;
import tech.illuin.wombat.module.llm_vertexai.connector.GoogleRegion;
import tech.illuin.wombat.module.llm_vertexai.connector.model.GoogleQueryResponse;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class LLMVertexAIActivityResolver implements WombatActivityResolver
{
    private final GoogleMultiClient client;

    private static final Logger logger = LoggerFactory.getLogger(LLMVertexAIActivityResolver.class);

    public LLMVertexAIActivityResolver(GoogleMultiClient client)
    {
        this.client = client;
    }

    @Override
    public boolean accept(Asset asset)
    {
        return asset instanceof LLMVertexAIAsset;
    }

    @Override
    public Optional<AssetActivity> resolve(Asset asset, TimeRange range, AssetFilter filter) throws WombatActivityException
    {
        LLMVertexAIAsset vertexAsset = (LLMVertexAIAsset) asset;
        LLMVertexAIProfile profile = vertexAsset.profile();

        Set<String> filteredServiceIds = filter == null ? Set.of() : filter.filterServiceIds(asset);
        Set<String> models = new HashSet<>(profile.models());
        if (!filteredServiceIds.isEmpty())
        {
            if (models.isEmpty())
                models.addAll(filteredServiceIds);
            else
                models.retainAll(filteredServiceIds);
            if (models.isEmpty())
                return Optional.empty();
        }

        requireBounded(asset, range);

        List<TimeRange> slots = range.split();
        if (slots.isEmpty())
            return Optional.empty();

        Map<String, ModelUsage> usage = this.usage(vertexAsset, range, models);

        List<ActivityData> activities = slots.stream()
            .map(slot -> activity(vertexAsset, slot, usage, models))
            .flatMap(Optional::stream)
            .toList();

        return activities.isEmpty() ? Optional.empty() : Optional.of(new AssetActivity(asset.identity(), activities));
    }

    private static void requireBounded(Asset asset, TimeRange range) throws WombatActivityException
    {
        if (range.start().equals(Instant.MIN) || range.end().equals(Instant.MAX))
            throw new WombatActivityException("Google usage cannot be queried over an unbounded range for asset " + asset.identity().id());
    }

    private Map<String, ModelUsage> usage(LLMVertexAIAsset asset, TimeRange range, Set<String> models) throws WombatActivityException
    {
        String assetId = asset.identity().id();
        GoogleClient googleClient = this.client.get(assetId)
            .orElseThrow(() -> new IllegalStateException("No Google client registered for asset " + assetId));

        String filter = buildFilter(models);
        String period = range.resolution().toSeconds() + "s";

        logger.debug("Fetching Google usage for asset {} over [{}, {}] aligned on {} with filter '{}'", assetId, range.start(), range.end(), period, filter);

        Map<String, ModelUsage> usage = new TreeMap<>();
        String pageToken = null;
        try {
            do {
                GoogleQueryResponse response = googleClient.query(
                    asset.resourceName(),
                    filter,
                    range.start().toString(),
                    range.end().toString(),
                    period,
                    pageToken
                );
                if (response == null)
                    break;

                if (response.timeSeries() != null)
                    extractTimeSeriesPoints(assetId, models, response.timeSeries(), usage);

                pageToken = response.nextPageToken();
            }
            while (pageToken != null && !pageToken.isBlank());
        }
        catch (FeignException e) {
            throw new WombatActivityException("Google usage query failed for asset " + assetId + ": " + e.getMessage(), e);
        }
        return usage;
    }

    private static String buildFilter(Set<String> models)
    {
        String filter = "metric.type=\"aiplatform.googleapis.com/publisher/online_serving/token_count\" AND metric.labels.type=\"output\"";
        if (!models.isEmpty())
        {
            filter += " AND (" + models.stream().sorted()
                .map(model -> "resource.labels.model_user_id=\"" + model.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(" OR ")) + ")";
        }
        return filter;
    }

    private static void extractTimeSeriesPoints(
        String assetId,
        Set<String> models,
        List<GoogleQueryResponse.TimeSeries> timeSeriesList,
        Map<String, ModelUsage> usage
    ) throws WombatActivityException
    {
        for (GoogleQueryResponse.TimeSeries series : timeSeriesList)
        {
            if (series.points() == null)
                continue;
            if (series.resource() == null || series.resource().labels() == null || series.resource().labels().model() == null || series.resource().labels().model().isBlank())
                throw new WombatActivityException("Google usage response has no model label for asset " + assetId);

            String model = series.resource().labels().model();
            if (!models.isEmpty() && !models.contains(model))
                continue;

            Country location = GoogleRegion.fromIdentifier(series.resource().labels().location())
                .map(GoogleRegion::country)
                .orElse(Country.WORLD);

            ModelUsage modelUsage = usage.computeIfAbsent(model, key -> new ModelUsage(location, new TreeMap<>()));
            for (GoogleQueryResponse.Point point : series.points())
            {
                Optional<Instant> pointEnd = point.endTime();
                if (pointEnd.isEmpty())
                    continue;
                modelUsage.points().merge(pointEnd.get(), Math.max(0L, Math.round(point.numericValue())), Long::sum);
            }
        }
    }

    private static long tokensIn(TreeMap<Instant, Long> tokensPerPointEnd, TimeRange slot)
    {
        return tokensPerPointEnd.subMap(slot.start(), false, slot.end(), true).values().stream()
            .mapToLong(Long::longValue)
            .sum();
    }

    private static Optional<ActivityData> activity(LLMVertexAIAsset asset, TimeRange slot, Map<String, ModelUsage> usage, Set<String> models)
    {
        LLMVertexAIProfile profile = asset.profile();
        Map<String, LLMServiceActivity> services = new LinkedHashMap<>();
        Set<String> resolvedModels = models.isEmpty() ? usage.keySet() : models;
        resolvedModels.stream().sorted().forEach(model -> {
            ModelUsage modelUsage = usage.get(model);
            Country location = modelUsage == null ? Country.WORLD : modelUsage.location();
            long tokens = modelUsage == null ? 0L : tokensIn(modelUsage.points(), slot);
            services.put(model, new LLMServiceActivity(LLMProvider.google_genai, model, location, tokens, (double) tokens / profile.outputTokensPerRequest()));
        });
        return services.isEmpty()
            ? Optional.empty()
            : Optional.of(new LLMActivityData(asset.type().regime(), services.keySet(), slot, services))
        ;
    }

    private record ModelUsage(Country location, TreeMap<Instant, Long> points) {}
}
