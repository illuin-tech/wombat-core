package tech.illuin.wombat.module.llm_google.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.source.WombatSource;
import tech.illuin.wombat.core.source.data.LLMData;
import tech.illuin.wombat.core.source.data.MetricData;
import tech.illuin.wombat.module.llm_google.LLMGoogleAsset;
import tech.illuin.wombat.module.llm_google.connector.GoogleClient;
import tech.illuin.wombat.module.llm_google.connector.GoogleMultiClient;
import tech.illuin.wombat.module.llm_google.connector.model.GoogleQueryResponse;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static java.util.Collections.emptyList;

public class LLMGoogleSource implements WombatSource
{
    private final GoogleMultiClient client;

    private static final Logger logger = LoggerFactory.getLogger(LLMGoogleSource.class);

    public LLMGoogleSource(GoogleMultiClient client)
    {
        this.client = client;
    }

    @Override
    public boolean accept(Asset asset)
    {
        return asset instanceof LLMGoogleAsset;
    }

    @Override
    public List<MetricData> source(Instant heartbeat, Asset asset)
    {
        LLMGoogleAsset googleAsset = (LLMGoogleAsset) asset;

        String assetId = asset.identity().id();
        GoogleClient googleClient = this.client.get(assetId)
            .orElseThrow(() -> new IllegalStateException("No Google client registered for asset " + assetId));

        String filter = googleAsset.profile().dynamicProfile().filter();
        Duration window = googleAsset.profile().dynamicProfile().windowOrDefault();
        Instant end = heartbeat.truncatedTo(ChronoUnit.SECONDS);
        Instant start = end.minus(window);

        logger.debug("Sampling output tokens for LLM asset {} over [{}, {}] with filter '{}'", assetId, start, end, filter);

        GoogleQueryResponse response = googleClient.query(
            googleAsset.projectId(),
            filter,
            start.toString(),
            end.toString(),
            window.toSeconds() + "s"
        );

        if (response == null)
        {
            logger.warn("Google query for LLM asset {} returned no response", assetId);
            return emptyList();
        }

        long outputTokens = Math.max(0L, Math.round(response.sum()));

        return List.of(new LLMData(
            googleAsset.profile().model(),
            googleAsset.profile().provider(),
            googleAsset.profile().model(),
            googleAsset.profile().location(),
            outputTokens,
            1
        ));
    }
}
