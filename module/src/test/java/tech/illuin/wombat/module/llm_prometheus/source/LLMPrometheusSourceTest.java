package tech.illuin.wombat.module.llm_prometheus.source;

import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.core.source.data.LLMData;
import tech.illuin.wombat.core.source.data.MetricData;
import tech.illuin.wombat.module.llm_prometheus.LLMPrometheusAsset;
import tech.illuin.wombat.module.llm_prometheus.LLMPrometheusProfile;
import tech.illuin.wombat.module.llm_prometheus.connector.PrometheusClient;
import tech.illuin.wombat.module.llm_prometheus.connector.PrometheusMultiClient;
import tech.illuin.wombat.module.llm_prometheus.connector.model.PrometheusQueryResponse;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LLMPrometheusSourceTest
{
    @Test
    void sample_emitsLLMDataWithProviderModelAndLocation()
    {
        PrometheusClient client = mock(PrometheusClient.class);
        LLMPrometheusProfile profile = new LLMPrometheusProfile(
            LLMProvider.mistralai,
            "mistral-large-latest",
            "FRA",
            new LLMPrometheusProfile.DynamicProfile("sum(vllm:num_tokens_total)")
        );
        LLMPrometheusAsset asset = new LLMPrometheusAsset(
            AssetIdentity.of("llm-prom", "prod", "Prom LLM"), "http://localhost:9090", null, null, null, 0, profile
        );

        PrometheusQueryResponse.Result res = new PrometheusQueryResponse.Result(
            null,
            List.of(new TextNode("1700000000"), new TextNode("420.0"))
        );
        PrometheusQueryResponse.Data data = new PrometheusQueryResponse.Data("vector", List.of(res));
        PrometheusQueryResponse response = new PrometheusQueryResponse("success", data);

        when(client.query("sum(vllm:num_tokens_total)")).thenReturn(response);

        PrometheusMultiClient multiClient = new PrometheusMultiClient();
        multiClient.register("llm-prom", client);
        LLMPrometheusSource source = new LLMPrometheusSource(multiClient);

        List<MetricData> dataList = source.source(Instant.now(), (Asset) asset);
        assertEquals(1, dataList.size());
        assertInstanceOf(LLMData.class, dataList.getFirst());

        LLMData llmData = (LLMData) dataList.getFirst();
        assertEquals("mistral-large-latest", llmData.serviceId());
        assertEquals(LLMProvider.mistralai, llmData.provider());
        assertEquals("mistral-large-latest", llmData.model());
        assertEquals("FRA", llmData.location());
        assertEquals(420L, llmData.outputTokens());
    }
}
