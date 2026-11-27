package tech.illuin.wombat.module.llm_prometheus.source;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.module.llm_prometheus.LLMPrometheusAsset;
import tech.illuin.wombat.module.llm_prometheus.connector.PrometheusClient;
import tech.illuin.wombat.module.llm_prometheus.connector.model.PrometheusQueryResponse;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.secret.MissingSecretException;
import tech.illuin.wombat.core.source.WombatSource;
import tech.illuin.wombat.core.source.WombatSourceException;
import tech.illuin.wombat.core.source.data.LLMData;
import tech.illuin.wombat.core.source.data.MetricData;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

public class LLMPrometheusSource implements WombatSource
{
    private final PrometheusClient client;

    private static final Logger logger = LoggerFactory.getLogger(LLMPrometheusSource.class);

    public LLMPrometheusSource(PrometheusClient client)
    {
        this.client = client;
    }

    @Override
    public boolean accept(Asset asset)
    {
        return asset instanceof LLMPrometheusAsset;
    }

    @Override
    public List<MetricData> source(Instant heartbeat, Instant lastActivation, Asset asset) throws WombatSourceException
    {
        LLMPrometheusAsset prometheusAsset = (LLMPrometheusAsset) asset;

        String assetId = asset.identity().id();
        String query = prometheusAsset.profile().dynamicProfile().query();
        logger.debug("Sampling output tokens for LLM asset {} with query '{}'", assetId, query);

        PrometheusQueryResponse response;
        try {
            response = this.client.query(query);
        }
        catch (FeignException | MissingSecretException e) {
            throw new WombatSourceException("Prometheus query failed for asset " + assetId + ": " + e.getMessage(), e);
        }

        Optional<LLMData> data = Optional.ofNullable(response)
            .map(res -> res.firstValue().orElse(0.0d))
            .map(value -> Math.max(0L, Math.round(value)))
            .map(outputTokens -> new LLMData(
                prometheusAsset.profile().model(),
                prometheusAsset.profile().provider(),
                prometheusAsset.profile().model(),
                prometheusAsset.profile().location(),
                outputTokens,
                1 //TODO: this should be modeled, at least as an optional metric ; we could use different strategies such "as-reported" or "inferred based on typical output-token sizes"
            ));

        if (data.isPresent())
            return List.of(data.get());

        logger.warn("Prometheus query for LLM asset {} returned no data, recording 0 tokens", assetId);
        return emptyList();
    }
}
