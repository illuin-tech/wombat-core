package tech.illuin.wombat.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.ProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tech.illuin.wombat.asset.model.profile.DynamicLLMProfile;
import tech.illuin.wombat.ecologits.model.EcologitsEstimationRequest;
import tech.illuin.wombat.persistence.ModelMetricRepository;
import tech.illuin.wombat.persistence.model.MetricData;
import tech.illuin.wombat.persistence.model.ModelMetricEntity;
import tech.illuin.wombat.prometheus.PrometheusClient;
import tech.illuin.wombat.prometheus.PrometheusMultiClientApi;
import tech.illuin.wombat.prometheus.model.PrometheusQueryResponse;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LLMMetricsCollectorTest
{

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ASSET_ID = "mistral-dynamic";

    private PrometheusClient prometheusClient;
    private ModelMetricRepository repository;
    private LLMMetricsCollector collector;

    @BeforeEach
    void setup()
    {
        this.prometheusClient = mock(PrometheusClient.class);
        PrometheusMultiClientApi multiClientApi = new PrometheusMultiClientApi();
        multiClientApi.register(ASSET_ID, this.prometheusClient);
        this.repository = mock(ModelMetricRepository.class);
        this.collector = new LLMMetricsCollector(multiClientApi, this.repository);
    }

    @Test
    void collect_parsesVectorValueAndPersistsRow() throws Exception
    {
        when(this.prometheusClient.query("q")).thenReturn(vector("1234.7"));

        this.collector.collect(Instant.ofEpochMilli(5000), assetConfig(), dynamicProfile("q"));

        ModelMetricEntity saved = capturedRow();
        assertEquals(5000L, saved.instantMs);
        assertEquals(new MetricData.LLMData(ASSET_ID, "mistral-large-latest"), saved.data);
        assertEquals(1235L, saved.outputTokens, "prometheus value is rounded to the nearest token");
    }

    @Test
    void collect_emptyResult_recordsZero() throws Exception
    {
        when(this.prometheusClient.query("q")).thenReturn(MAPPER.readValue(
            "{\"status\":\"success\",\"data\":{\"resultType\":\"vector\",\"result\":[]}}", PrometheusQueryResponse.class));

        this.collector.collect(Instant.ofEpochMilli(5000), assetConfig(), dynamicProfile("q"));

        assertEquals(0L, capturedRow().outputTokens);
    }

    @Test
    void collect_queryThrows_isSwallowedAndNothingPersisted()
    {
        when(this.prometheusClient.query("q")).thenThrow(new ProcessingException("prometheus down"));

        this.collector.collect(Instant.ofEpochMilli(5000), assetConfig(), dynamicProfile("q"));

        verify(this.repository, never()).save(any());
    }

    @Test
    void collect_unregisteredAsset_throws()
    {
        LLMPrometheusProperties unknown = new LLMPrometheusProperties("unknown", "Unknown", "http://prometheus", null, null, null, 0, dynamicProfile("q"));

        assertThrows(IllegalStateException.class,
            () -> this.collector.collect(Instant.ofEpochMilli(5000), unknown, dynamicProfile("q")));
    }

    private ModelMetricEntity capturedRow()
    {
        ArgumentCaptor<ModelMetricEntity> captor = ArgumentCaptor.forClass(ModelMetricEntity.class);
        verify(this.repository).save(captor.capture());
        return captor.getValue();
    }

    private static LLMPrometheusProperties assetConfig()
    {
        return new LLMPrometheusProperties(ASSET_ID, "Mistral dynamic", "http://prometheus", null, null, null, 0, dynamicProfile("q"));
    }

    private static PrometheusQueryResponse vector(String value) throws Exception
    {
        return MAPPER.readValue(
            "{\"status\":\"success\",\"data\":{\"resultType\":\"vector\",\"result\":["
            + "{\"metric\":{},\"value\":[1435781451.781,\"" + value + "\"]}]}}",
            PrometheusQueryResponse.class);
    }

    private static DynamicLLMProfile dynamicProfile(String query)
    {
        return new DynamicLLMProfile(
            EcologitsEstimationRequest.Provider.mistralai, "mistral-large-latest", "FRA",
            new DynamicLLMProfile.DynamicProfile(query));
    }
}
