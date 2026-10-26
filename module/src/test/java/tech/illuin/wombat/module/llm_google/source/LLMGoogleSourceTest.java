package tech.illuin.wombat.module.llm_google.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.core.source.data.LLMData;
import tech.illuin.wombat.core.source.data.MetricData;
import tech.illuin.wombat.module.llm_google.LLMGoogleAsset;
import tech.illuin.wombat.module.llm_google.LLMGoogleProfile;
import tech.illuin.wombat.module.llm_google.connector.GoogleClient;
import tech.illuin.wombat.module.llm_google.connector.GoogleMultiClient;
import tech.illuin.wombat.module.llm_google.connector.model.GoogleQueryResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LLMGoogleSourceTest
{
    private static final String PROJECT = "macro-theater-402700";
    private static final String FILTER = "metric.type=\"aiplatform.googleapis.com/publisher/online_serving/token_count\" AND metric.labels.type=\"output\"";
    private static final Instant HEARTBEAT = Instant.parse("2026-10-06T21:00:00Z");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void sample_emitsLLMDataWithProviderModelAndLocation() throws Exception
    {
        GoogleClient client = mock(GoogleClient.class);
        when(client.query(PROJECT, FILTER, "2026-10-06T20:59:00Z", "2026-10-06T21:00:00Z", "60s"))
            .thenReturn(response("{\"timeSeries\":[{\"points\":[{\"value\":{\"int64Value\":\"1576\"}}]}]}"));

        List<MetricData> dataList = source(client).source(HEARTBEAT, (Asset) asset(null));

        assertEquals(1, dataList.size());
        assertInstanceOf(LLMData.class, dataList.getFirst());

        LLMData llmData = (LLMData) dataList.getFirst();
        assertEquals("gemini-2.5-flash", llmData.serviceId());
        assertEquals(LLMProvider.google_genai, llmData.provider());
        assertEquals("gemini-2.5-flash", llmData.model());
        assertEquals(Country.US, llmData.location());
        assertEquals(1576L, llmData.outputTokens());
        assertEquals(1L, llmData.requests());
    }

    @Test
    void sample_sumsTheTokensOfEverySeriesAndPoint() throws Exception
    {
        GoogleClient client = mock(GoogleClient.class);
        when(client.query(any(), any(), any(), any(), any())).thenReturn(response("""
            {"timeSeries":[
              {"points":[{"value":{"int64Value":"1000"}},{"value":{"int64Value":"500"}}]},
              {"points":[{"value":{"int64Value":"76"}}]}
            ]}
            """));

        LLMData llmData = (LLMData) source(client).source(HEARTBEAT, asset(null)).getFirst();

        assertEquals(1576L, llmData.outputTokens());
    }

    @Test
    void sample_recordsZeroTokensWhenNothingMatchedInTheWindow() throws Exception
    {
        GoogleClient client = mock(GoogleClient.class);
        when(client.query(any(), any(), any(), any(), any())).thenReturn(response("{}"));

        List<MetricData> dataList = source(client).source(HEARTBEAT, asset(null));

        assertEquals(1, dataList.size());
        assertEquals(0L, ((LLMData) dataList.getFirst()).outputTokens());
    }

    @Test
    void sample_emitsNothingWhenTheClientReturnsNoResponse()
    {
        GoogleClient client = mock(GoogleClient.class);
        when(client.query(any(), any(), any(), any(), any())).thenReturn(null);

        assertTrue(source(client).source(HEARTBEAT, asset(null)).isEmpty());
    }

    @Test
    void sample_queriesAWindowEndingAtTheHeartbeat() throws Exception
    {
        GoogleClient client = mock(GoogleClient.class);
        when(client.query(PROJECT, FILTER, "2026-10-06T20:55:00Z", "2026-10-06T21:00:00Z", "300s"))
            .thenReturn(response("{\"timeSeries\":[{\"points\":[{\"value\":{\"int64Value\":\"42\"}}]}]}"));

        List<MetricData> dataList = source(client).source(HEARTBEAT, asset(Duration.ofMinutes(5)));

        assertEquals(42L, ((LLMData) dataList.getFirst()).outputTokens());
    }

    @Test
    void sample_truncatesTheHeartbeatToWholeSeconds() throws Exception
    {
        GoogleClient client = mock(GoogleClient.class);
        when(client.query(PROJECT, FILTER, "2026-10-06T20:59:00Z", "2026-10-06T21:00:00Z", "60s"))
            .thenReturn(response("{\"timeSeries\":[{\"points\":[{\"value\":{\"int64Value\":\"7\"}}]}]}"));

        List<MetricData> dataList = source(client).source(Instant.parse("2026-10-06T21:00:00.789Z"), asset(null));

        assertEquals(7L, ((LLMData) dataList.getFirst()).outputTokens());
    }

    @Test
    void sample_failsWhenNoClientIsRegisteredForTheAsset()
    {
        LLMGoogleSource source = new LLMGoogleSource(new GoogleMultiClient());

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> source.source(HEARTBEAT, asset(null)));

        assertTrue(e.getMessage().contains("llm-google"), e.getMessage());
    }

    @Test
    void acceptsOnlyGoogleAssets()
    {
        LLMGoogleSource source = source(mock(GoogleClient.class));

        assertTrue(source.accept(asset(null)));
        assertFalse(source.accept(mock(Asset.class)));
    }

    private static GoogleQueryResponse response(String json) throws Exception
    {
        return MAPPER.readValue(json, GoogleQueryResponse.class);
    }

    private static LLMGoogleSource source(GoogleClient client)
    {
        GoogleMultiClient multiClient = new GoogleMultiClient();
        multiClient.register("llm-google", client);
        return new LLMGoogleSource(multiClient);
    }

    private static LLMGoogleAsset asset(Duration window)
    {
        return new LLMGoogleAsset(
            AssetIdentity.of("llm-google", "prod", "Google LLM"), "https://monitoring.googleapis.com", PROJECT, null, 0,
            new LLMGoogleProfile(LLMProvider.google_genai, "gemini-2.5-flash", Country.US, new LLMGoogleProfile.DynamicProfile(FILTER, window))
        );
    }
}
