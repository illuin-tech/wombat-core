package tech.illuin.wombat.llm;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.persistence.ModelMetricRepository;
import tech.illuin.wombat.persistence.model.MetricData;
import tech.illuin.wombat.persistence.model.ModelMetricEntity;
import tech.illuin.wombat.prometheus.PrometheusClient;
import tech.illuin.wombat.prometheus.PrometheusMultiClientApi;
import tech.illuin.wombat.prometheus.model.PrometheusQueryResponse;
import tech.illuin.wombat.asset.model.profile.DynamicLLMProfile;

import java.time.Instant;
import java.util.OptionalDouble;

public class LLMMetricsCollector
{

    private static final Logger logger = LoggerFactory.getLogger(LLMMetricsCollector.class);

    private final PrometheusMultiClientApi multiClientApi;
    private final ModelMetricRepository repository;

    public LLMMetricsCollector(PrometheusMultiClientApi multiClientApi, ModelMetricRepository repository)
    {
        this.multiClientApi = multiClientApi;
        this.repository = repository;
    }

    public void collect(Instant instant, LLMPrometheusProperties assetConfig, DynamicLLMProfile profile)
    {
        String assetId = assetConfig.id();
        PrometheusClient prometheusClient = this.multiClientApi.get(assetId)
            .orElseThrow(() -> new IllegalStateException("No Prometheus client registered for asset " + assetId));

        String query = profile.dynamicProfile().query();
        logger.debug("Sampling output tokens for LLM asset {} with query '{}'", assetId, query);
        try
        {
            PrometheusQueryResponse response = prometheusClient.query(query);
            OptionalDouble value = response == null ? OptionalDouble.empty() : response.firstValue();
            if (value.isEmpty())
            {
                logger.warn("Prometheus query for LLM asset {} returned no data, recording 0 tokens", assetId);
            }
            long outputTokens = Math.max(0L, Math.round(value.orElse(0.0)));
            this.repository.save(row(instant, assetId, profile, outputTokens));
        }
        catch (ProcessingException | WebApplicationException e) {
            logger.warn("Failed to sample output tokens for LLM asset {}: {}", assetId, e.getMessage());
        }
    }

    private static ModelMetricEntity row(Instant instant, String assetId, DynamicLLMProfile profile, long outputTokens)
    {
        ModelMetricEntity entity = new ModelMetricEntity();
        entity.instantMs = instant.toEpochMilli();
        entity.data = new MetricData.LLMData(assetId, profile.model());
        entity.outputTokens = outputTokens;
        return entity;
    }
}
