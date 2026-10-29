package tech.illuin.wombat.module.llm_anthropic.activity;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.core.activity.WombatActivityException;
import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.commons.AssetFilter;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.activity.llm.LLMActivityData;
import tech.illuin.wombat.core.activity.llm.LLMServiceActivity;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.module.llm_anthropic.LLMAnthropicAsset;
import tech.illuin.wombat.module.llm_anthropic.LLMAnthropicProfile;
import tech.illuin.wombat.module.llm_anthropic.connector.AnthropicAdminClient;
import tech.illuin.wombat.module.llm_anthropic.connector.AnthropicMultiClient;
import tech.illuin.wombat.module.llm_anthropic.connector.model.AnthropicUsageReport;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class LLMAnthropicActivityResolver implements WombatActivityResolver
{
    private final AnthropicMultiClient client;

    private static final Logger logger = LoggerFactory.getLogger(LLMAnthropicActivityResolver.class);

    public LLMAnthropicActivityResolver(AnthropicMultiClient client)
    {
        this.client = client;
    }

    @Override
    public boolean accept(Asset asset)
    {
        return asset instanceof LLMAnthropicAsset;
    }

    @Override
    public ActivityResolverType activityResolverType()
    {
        return ActivityResolverType.EXTERNAL_API;
    }

    @Override
    public Optional<ActivityData> resolve(Asset asset, TimeRange range, AssetFilter filter) throws WombatActivityException
    {
        LLMAnthropicProfile profile = ((LLMAnthropicAsset) asset).profile();
        Map<String, Map<Long, Long>> usage = this.usage((LLMAnthropicAsset) asset, range, BucketWidth.forRange(range));

        Set<String> filteredServiceIds = filter == null ? Set.of() : filter.filterServiceIds(asset);
        Map<String, LLMServiceActivity> activities = new LinkedHashMap<>();
        usage.forEach((model, perBucket) -> {
            if (!filteredServiceIds.isEmpty() && !filteredServiceIds.contains(model))
                return;
            long tokens = perBucket.values().stream().mapToLong(Long::longValue).sum();
            activities.put(model, new LLMServiceActivity(
                LLMProvider.anthropic,
                model,
                profile.location(),
                tokens,
                (double) tokens / profile.outputTokensPerRequest()
            ));
        });

        Set<String> resolvedServiceIds = filteredServiceIds.isEmpty() ? activities.keySet() : filteredServiceIds;
        return Optional.of(new LLMActivityData(ActivityRegime.MEASURED, resolvedServiceIds, range, activities));
    }

    @Override
    public Map<Long, Double> activityPerBucket(Asset asset, TimeRange range, long stepMs, Collection<String> serviceIds) throws WombatActivityException
    {
        Map<String, Map<Long, Long>> usage = this.usage((LLMAnthropicAsset) asset, range, BucketWidth.forStep(stepMs));

        Map<Long, Double> perStep = new TreeMap<>();
        usage.forEach((model, perBucket) -> {
            if (serviceIds != null && !serviceIds.isEmpty() && !serviceIds.contains(model))
                return;
            perBucket.forEach((bucketStart, tokens) ->
                perStep.merge(bucketStart - Math.floorMod(bucketStart, stepMs), tokens.doubleValue(), Double::sum)
            );
        });
        return perStep;
    }

    private Map<String, Map<Long, Long>> usage(LLMAnthropicAsset asset, TimeRange range, BucketWidth width) throws WombatActivityException
    {
        String assetId = asset.identity().id();
        AnthropicAdminClient adminClient = this.client.get(assetId)
            .orElseThrow(() -> new IllegalStateException("No Anthropic client registered for asset " + assetId));

        return fetch(adminClient, asset.profile(), range.start(), range.end(), width, assetId);
    }

    private static Map<String, Map<Long, Long>> fetch(AnthropicAdminClient adminClient, LLMAnthropicProfile profile, Instant start, Instant end, BucketWidth width, String assetId) throws WombatActivityException
    {
        logger.debug("Fetching Anthropic usage for asset {} over [{}, {}) with {} buckets", assetId, start, end, width.code);

        Map<String, Map<Long, Long>> usage = new TreeMap<>();
        String page = null;
        try {
            do {
                AnthropicUsageReport report = adminClient.usageReport(
                    start.toString(),
                    end.toString(),
                    width.code,
                    width.limit,
                    nullIfEmpty(profile.models()),
                    nullIfEmpty(profile.workspaceIds()),
                    nullIfEmpty(profile.apiKeyIds()),
                    page
                );
                if (report == null || report.data() == null)
                    break;

                for (AnthropicUsageReport.Bucket bucket : report.data())
                {
                    if (bucket.results() == null || bucket.results().isEmpty() || bucket.startingAt() == null)
                        continue;
                    long bucketStart = Instant.parse(bucket.startingAt()).toEpochMilli();
                    for (AnthropicUsageReport.Result result : bucket.results())
                    {
                        if (result.model() == null)
                            continue;
                        usage.computeIfAbsent(result.model(), m -> new TreeMap<>())
                            .merge(bucketStart, result.outputTokens(), Long::sum);
                    }
                }
                page = report.hasMore() ? report.nextPage() : null;
            }
            while (page != null);
        }
        catch (FeignException e) {
            throw new WombatActivityException("Anthropic usage report failed for asset " + assetId + ": " + e.getMessage(), e);
        }
        return usage;
    }

    private static List<String> nullIfEmpty(List<String> values)
    {
        return values.isEmpty() ? null : values;
    }

    enum BucketWidth
    {
        HOUR("1h", 168, Duration.ofHours(1)),
        DAY("1d", 31, Duration.ofDays(1)),
        ;

        private final String code;
        private final int limit;
        private final Duration duration;

        BucketWidth(String code, int limit, Duration duration)
        {
            this.code = code;
            this.limit = limit;
            this.duration = duration;
        }

        static BucketWidth forRange(TimeRange range)
        {
            return Duration.between(range.start(), range.end()).compareTo(TimeRange.HOURLY_MAX_SPAN) <= 0 ? HOUR : DAY;
        }

        static BucketWidth forStep(long stepMs)
        {
            if (stepMs > 0 && stepMs % DAY.duration.toMillis() == 0)
                return DAY;
            if (stepMs > 0 && stepMs % HOUR.duration.toMillis() == 0)
                return HOUR;
            throw new IllegalArgumentException("Anthropic usage is bucketed by hour or day, got a step of " + stepMs + "ms");
        }
    }
}
