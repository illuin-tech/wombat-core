package tech.illuin.wombat.module.llm_google.activity;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.core.activity.WombatActivityException;
import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.commons.AssetActivity;
import tech.illuin.wombat.core.activity.commons.AssetFilter;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.activity.llm.LLMActivityData;
import tech.illuin.wombat.core.activity.llm.LLMServiceActivity;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.module.llm_google.LLMGoogleAsset;
import tech.illuin.wombat.module.llm_google.LLMGoogleProfile;
import tech.illuin.wombat.module.llm_google.connector.GoogleClient;
import tech.illuin.wombat.module.llm_google.connector.GoogleMultiClient;
import tech.illuin.wombat.module.llm_google.connector.model.GoogleQueryResponse;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class LLMGoogleActivityResolver implements WombatActivityResolver
{
    private final GoogleMultiClient client;

    private static final Logger logger = LoggerFactory.getLogger(LLMGoogleActivityResolver.class);

    public LLMGoogleActivityResolver(GoogleMultiClient client)
    {
        this.client = client;
    }

    @Override
    public boolean accept(Asset asset)
    {
        return asset instanceof LLMGoogleAsset;
    }

    @Override
    public Optional<AssetActivity> resolve(Asset asset, TimeRange range, AssetFilter filter) throws WombatActivityException
    {
        LLMGoogleAsset googleAsset = (LLMGoogleAsset) asset;
        LLMGoogleProfile profile = googleAsset.profile();

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

        Map<String, TreeMap<Instant, Long>> usage = this.usage(googleAsset, range, models);

        List<ActivityData> activities = slots.stream()
            .map(slot -> activity(googleAsset, slot, usage, models))
            .flatMap(Optional::stream)
            .toList();

        return activities.isEmpty() ? Optional.empty() : Optional.of(new AssetActivity(asset.identity(), activities));
    }

    private static void requireBounded(Asset asset, TimeRange range) throws WombatActivityException
    {
        if (range.start().equals(Instant.MIN) || range.end().equals(Instant.MAX))
            throw new WombatActivityException("Google usage cannot be queried over an unbounded range for asset " + asset.identity().id());
    }

    private Map<String, TreeMap<Instant, Long>> usage(LLMGoogleAsset asset, TimeRange range, Set<String> models) throws WombatActivityException
    {
        String assetId = asset.identity().id();
        GoogleClient googleClient = this.client.get(assetId)
            .orElseThrow(() -> new IllegalStateException("No Google client registered for asset " + assetId));

        String filter = "metric.type=\"aiplatform.googleapis.com/publisher/online_serving/token_count\" AND metric.labels.type=\"output\"";
        if (!models.isEmpty())
            filter += " AND (" + models.stream().sorted()
                .map(model -> "resource.labels.model_user_id=\"" + model.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(" OR ")) + ")";
        String period = range.resolution().toSeconds() + "s";

        logger.debug("Fetching Google usage for asset {} over [{}, {}] aligned on {} with filter '{}'", assetId, range.start(), range.end(), period, filter);

        Map<String, TreeMap<Instant, Long>> usage = new TreeMap<>();
        String page = null;
        try {
            do {
                GoogleQueryResponse response = googleClient.query(
                    asset.resourceName(),
                    filter,
                    range.start().toString(),
                    range.end().toString(),
                    period,
                    page
                );
                if (response == null)
                    break;

                if (response.timeSeries() != null)
                {
                    for (GoogleQueryResponse.TimeSeries series : response.timeSeries())
                    {
                        if (series.points() == null)
                            continue;
                        if (series.resource() == null || series.resource().labels() == null
                            || series.resource().labels().model() == null || series.resource().labels().model().isBlank())
                            throw new WombatActivityException("Google usage response has no model label for asset " + assetId);
                        String model = series.resource().labels().model();
                        if (!models.isEmpty() && !models.contains(model))
                            continue;
                        TreeMap<Instant, Long> tokensPerPointEnd = usage.computeIfAbsent(model, key -> new TreeMap<>());
                        for (GoogleQueryResponse.Point point : series.points())
                        {
                            Optional<Instant> pointEnd = point.endTime();
                            if (pointEnd.isEmpty())
                                continue;
                            tokensPerPointEnd.merge(pointEnd.get(), Math.max(0L, Math.round(point.numericValue())), Long::sum);
                        }
                    }
                }
                page = response.nextPageToken() == null || response.nextPageToken().isBlank() ? null : response.nextPageToken();
            }
            while (page != null);
        }
        catch (FeignException e) {
            throw new WombatActivityException("Google usage query failed for asset " + assetId + ": " + e.getMessage(), e);
        }
        return usage;
    }

    private static long tokensIn(TreeMap<Instant, Long> tokensPerPointEnd, TimeRange slot)
    {
        return tokensPerPointEnd.subMap(slot.start(), false, slot.end(), true).values().stream()
            .mapToLong(Long::longValue)
            .sum();
    }

    private static Optional<ActivityData> activity(LLMGoogleAsset asset, TimeRange slot, Map<String, TreeMap<Instant, Long>> usage, Set<String> models)
    {
        LLMGoogleProfile profile = asset.profile();
        Map<String, LLMServiceActivity> services = new LinkedHashMap<>();
        Set<String> resolvedModels = models.isEmpty() ? usage.keySet() : models;
        resolvedModels.stream().sorted().forEach(model -> {
            TreeMap<Instant, Long> points = usage.get(model);
            long tokens = points == null ? 0L : tokensIn(points, slot);
            services.put(model, new LLMServiceActivity(profile.provider(), model, profile.location(), tokens, (double) tokens / profile.outputTokensPerRequest()));
        });
        return services.isEmpty() ? Optional.empty()
            : Optional.of(new LLMActivityData(asset.type().regime(), services.keySet(), slot, services));
    }
}
