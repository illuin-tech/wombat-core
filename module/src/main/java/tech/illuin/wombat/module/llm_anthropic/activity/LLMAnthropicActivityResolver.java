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

import java.time.Instant;
import java.util.*;

public class LLMAnthropicActivityResolver implements WombatActivityResolver
{
    private final AnthropicMultiClient client;

    private static final String BUCKET_WIDTH = "1h";
    private static final int BUCKET_LIMIT = 168;
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
    public Optional<ActivityData> resolve(Asset asset, TimeRange range, AssetFilter filter) throws WombatActivityException
    {
        LLMAnthropicAsset anthropicAsset = (LLMAnthropicAsset) asset;
        LLMAnthropicProfile profile = anthropicAsset.profile();
        String assetId = asset.identity().id();

        AnthropicAdminClient adminClient = this.client.get(assetId)
            .orElseThrow(() -> new IllegalStateException("No Anthropic client registered for asset " + assetId));

        Map<String, Long> outputTokens = this.outputTokensPerModel(adminClient, profile, range.start(), range.end(), assetId);

        Set<String> filteredServiceIds = filter == null ? Set.of() : filter.filterServiceIds(asset);
        Map<String, LLMServiceActivity> activities = new LinkedHashMap<>();
        outputTokens.forEach((model, tokens) -> {
            if (!filteredServiceIds.isEmpty() && !filteredServiceIds.contains(model))
                return;
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

    private Map<String, Long> outputTokensPerModel(AnthropicAdminClient adminClient, LLMAnthropicProfile profile, Instant start, Instant end, String assetId) throws WombatActivityException
    {
        logger.debug("Fetching Anthropic usage for asset {} over [{}, {})", assetId, start, end);

        Map<String, Long> tokens = new TreeMap<>();
        String page = null;
        try {
            do {
                AnthropicUsageReport report = adminClient.usageReport(
                    start.toString(),
                    end.toString(),
                    BUCKET_WIDTH,
                    BUCKET_LIMIT,
                    nullIfEmpty(profile.models()),
                    nullIfEmpty(profile.workspaceIds()),
                    nullIfEmpty(profile.apiKeyIds()),
                    page
                );
                if (report == null || report.data() == null)
                    break;

                for (AnthropicUsageReport.Bucket bucket : report.data())
                {
                    if (bucket.results() == null)
                        continue;
                    for (AnthropicUsageReport.Result result : bucket.results())
                    {
                        if (result.model() == null)
                            continue;
                        tokens.merge(result.model(), result.outputTokens(), Long::sum);
                    }
                }
                page = report.hasMore() ? report.nextPage() : null;
            }
            while (page != null);
        }
        catch (FeignException e) {
            throw new WombatActivityException("Anthropic usage report failed for asset " + assetId + ": " + e.getMessage(), e);
        }
        return tokens;
    }

    private static List<String> nullIfEmpty(List<String> values)
    {
        return values.isEmpty() ? null : values;
    }
}
