package tech.illuin.wombat.module.llm_vertexai.activity;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.module.llm_vertexai.LLMVertexAIModule;
import tech.illuin.wombat.core.activity.WombatActivityException;
import tech.illuin.wombat.core.activity.commons.AssetActivity;
import tech.illuin.wombat.core.activity.commons.AssetFilter;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.activity.llm.LLMActivityData;
import tech.illuin.wombat.core.activity.llm.LLMServiceActivity;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.module.llm_vertexai.LLMVertexAIAsset;
import tech.illuin.wombat.module.llm_vertexai.LLMVertexAIProfile;
import tech.illuin.wombat.module.llm_vertexai.connector.GoogleClient;
import tech.illuin.wombat.module.llm_vertexai.connector.GoogleMultiClient;
import tech.illuin.wombat.module.llm_vertexai.connector.model.GoogleQueryResponse;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class LLMVertexAIActivityResolverTest
{
    private static final String PROJECT = "projects/macro-theater-402700";
    private static final String BASE_FILTER = "metric.type=\"aiplatform.googleapis.com/publisher/online_serving/token_count\" AND metric.labels.type=\"output\"";
    private static final String FILTER = BASE_FILTER + " AND (resource.labels.model_user_id=\"gemini-2.5-flash\")";
    private static final String MODEL = "gemini-2.5-flash";
    private static final Instant START = Instant.parse("2026-10-06T20:00:00Z");
    private static final TimeRange THREE_HOURS = new TimeRange(START, START.plus(Duration.ofHours(3)), Duration.ofHours(1));
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final GoogleClient client = mock(GoogleClient.class);
    private final LLMVertexAIAsset asset = new LLMVertexAIAsset(
        AssetIdentity.of("llm-google", "prod", "Google LLM", LLMVertexAIModule.TYPE), "https://monitoring.googleapis.com", PROJECT, null,
        new LLMVertexAIProfile(500, List.of(MODEL))
    );
    private final LLMVertexAIActivityResolver resolver = new LLMVertexAIActivityResolver(multi(this.client));

    @Test
    void queriesTheWholeRangeOnceAlignedOnItsResolution() throws Exception
    {
        when(this.client.query(any(), any(), any(), any(), any(), any())).thenReturn(response("{}"));

        this.resolver.resolve(this.asset, THREE_HOURS, null);

        verify(this.client).query(PROJECT, FILTER, "2026-10-06T20:00:00Z", "2026-10-06T23:00:00Z", "3600s", null);
    }

    @Test
    void emitsOneActivityPerSlotWithProviderModelLocationAndRequests() throws Exception
    {
        when(this.client.query(any(), any(), any(), any(), any(), isNull())).thenReturn(response("""
            {"timeSeries":[{"resource":{"labels":{"model_user_id":"gemini-2.5-flash","location":"us-central1"}},"points":[
              {"interval":{"endTime":"2026-10-06T23:00:00Z"},"value":{"int64Value":"250"}},
              {"interval":{"endTime":"2026-10-06T21:00:00Z"},"value":{"int64Value":"1000"}}
            ]}]}
            """));

        AssetActivity activity = this.resolver.resolve(this.asset, THREE_HOURS, null).orElseThrow();

        assertEquals(this.asset.identity(), activity.assetIdentity());
        assertEquals(3, activity.activity().size());

        LLMActivityData first = (LLMActivityData) activity.activity().getFirst();
        assertEquals(ActivityRegime.MEASURED, first.regime());
        assertEquals(new TimeRange(START, START.plus(Duration.ofHours(1)), Duration.ofHours(1)), first.range());
        assertEquals(Set.of(MODEL), first.serviceIds());
        LLMServiceActivity service = first.services().get(MODEL);
        assertEquals(LLMProvider.google_genai, service.provider());
        assertEquals(MODEL, service.model());
        assertEquals(Country.US, service.location());
        assertEquals(1000L, service.outputTokenCount());
        assertEquals(2.0d, service.requestCount());

        assertEquals(0L, ((LLMActivityData) activity.activity().get(1)).outputTokenCount());
        assertEquals(250L, ((LLMActivityData) activity.activity().get(2)).outputTokenCount());
        assertEquals(0.5d, ((LLMActivityData) activity.activity().get(2)).requestCount());
    }

    @Test
    void sumsSeriesAndFollowsPagination() throws Exception
    {
        when(this.client.query(any(), any(), any(), any(), any(), isNull())).thenReturn(response("""
            {"timeSeries":[
              {"resource":{"labels":{"model_user_id":"gemini-2.5-flash"}},"points":[{"interval":{"endTime":"2026-10-06T21:00:00Z"},"value":{"int64Value":"100"}}]},
              {"resource":{"labels":{"model_user_id":"gemini-2.5-flash"}},"points":[{"interval":{"endTime":"2026-10-06T21:00:00Z"},"value":{"doubleValue":50.4}}]}
            ],"nextPageToken":"p2"}
            """));
        when(this.client.query(any(), any(), any(), any(), any(), eq("p2"))).thenReturn(response("""
            {"timeSeries":[{"resource":{"labels":{"model_user_id":"gemini-2.5-flash"}},"points":[{"interval":{"endTime":"2026-10-06T21:00:00Z"},"value":{"int64Value":"7"}}]}]}
            """));

        AssetActivity activity = this.resolver.resolve(this.asset, THREE_HOURS, null).orElseThrow();

        assertEquals(157L, ((LLMActivityData) activity.activity().getFirst()).outputTokenCount());
        verify(this.client, times(2)).query(any(), any(), any(), any(), any(), any());
    }

    @Test
    void ignoresPointsOutsideTheRangeOrWithoutInterval() throws Exception
    {
        when(this.client.query(any(), any(), any(), any(), any(), any())).thenReturn(response("""
            {"timeSeries":[{"resource":{"labels":{"model_user_id":"gemini-2.5-flash"}},"points":[
              {"interval":{"endTime":"2026-10-06T20:00:00Z"},"value":{"int64Value":"9"}},
              {"interval":{"endTime":"2026-10-06T23:00:01Z"},"value":{"int64Value":"9"}},
              {"value":{"int64Value":"9"}}
            ]}]}
            """));

        AssetActivity activity = this.resolver.resolve(this.asset, THREE_HOURS, null).orElseThrow();

        activity.activity().forEach(data -> assertEquals(0L, ((LLMActivityData) data).outputTokenCount()));
    }

    @Test
    void reportsZeroActivityWhenTheClientReturnsNoResponse() throws Exception
    {
        when(this.client.query(any(), any(), any(), any(), any(), any())).thenReturn(null);

        AssetActivity activity = this.resolver.resolve(this.asset, THREE_HOURS, null).orElseThrow();

        assertEquals(3, activity.activity().size());
        activity.activity().forEach(data -> assertEquals(0L, ((LLMActivityData) data).outputTokenCount()));
    }

    @Test
    void skipsTheQueryWhenTheFilterExcludesTheModel() throws Exception
    {
        AssetFilter filter = new AssetFilter(new AssetFilter.Environment("prod", Set.of(new AssetFilter.Asset("llm-google", Set.of("other-model")))));

        assertTrue(this.resolver.resolve(this.asset, THREE_HOURS, filter).isEmpty());
        verifyNoInteractions(this.client);
    }

    @Test
    void keepsTheModelWhenTheFilterSelectsIt() throws Exception
    {
        when(this.client.query(any(), any(), any(), any(), any(), any())).thenReturn(response("{}"));
        AssetFilter filter = new AssetFilter(new AssetFilter.Environment("prod", Set.of(new AssetFilter.Asset("llm-google", Set.of(MODEL)))));

        assertTrue(this.resolver.resolve(this.asset, THREE_HOURS, filter).isPresent());
    }

    @Test
    void rejectsAnUnboundedRange()
    {
        TimeRange unbounded = new TimeRange(Instant.MIN, START);

        assertThrows(WombatActivityException.class, () -> this.resolver.resolve(this.asset, unbounded, null));
        verifyNoInteractions(this.client);
    }

    @Test
    void wrapsApiFailures()
    {
        Request request = Request.create(Request.HttpMethod.GET, "/", Map.of(), null, StandardCharsets.UTF_8, null);
        when(this.client.query(any(), any(), any(), any(), any(), any()))
            .thenThrow(new FeignException.Forbidden("denied", request, null, Map.of()));

        WombatActivityException e = assertThrows(WombatActivityException.class, () -> this.resolver.resolve(this.asset, THREE_HOURS, null));

        assertTrue(e.getMessage().contains("llm-google"), e.getMessage());
    }

    @Test
    void failsWhenNoClientIsRegisteredForTheAsset()
    {
        LLMVertexAIActivityResolver resolver = new LLMVertexAIActivityResolver(new GoogleMultiClient());

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> resolver.resolve(this.asset, THREE_HOURS, null));

        assertTrue(e.getMessage().contains("llm-google"), e.getMessage());
    }

    @Test
    void acceptsOnlyVertexAIAssets()
    {
        assertTrue(this.resolver.accept(this.asset));
        assertFalse(this.resolver.accept(mock(Asset.class)));
    }

    @Test
    void discoversModelsAndKeepsTheirUsageSeparate() throws Exception
    {
        LLMVertexAIAsset allModels = new LLMVertexAIAsset(this.asset.identity(), this.asset.googleUrl(), "folders/123", null,
            new LLMVertexAIProfile(500, null));
        when(this.client.query(any(), any(), any(), any(), any(), any())).thenReturn(response("""
            {"timeSeries":[
              {"resource":{"labels":{"model_user_id":"gemini-2.5-flash"}},"points":[
                {"interval":{"endTime":"2026-10-06T21:00:00Z"},"value":{"int64Value":"1000"}}]},
              {"resource":{"labels":{"model_user_id":"gemini-2.5-pro"}},"points":[
                {"interval":{"endTime":"2026-10-06T21:00:00Z"},"value":{"int64Value":"250"}}]}
            ]}
            """));

        AssetActivity activity = this.resolver.resolve(allModels, THREE_HOURS, null).orElseThrow();
        LLMActivityData first = (LLMActivityData) activity.activity().getFirst();

        assertEquals(Set.of(MODEL, "gemini-2.5-pro"), first.serviceIds());
        assertEquals(1000L, first.services().get(MODEL).outputTokenCount());
        assertEquals(250L, first.services().get("gemini-2.5-pro").outputTokenCount());
        verify(this.client).query("folders/123", BASE_FILTER, START.toString(), THREE_HOURS.end().toString(), "3600s", null);
    }

    @Test
    void intersectsConfiguredModelsWithRequestedServices() throws Exception
    {
        LLMVertexAIAsset multiple = new LLMVertexAIAsset(this.asset.identity(), this.asset.googleUrl(), "organizations/456", null,
            new LLMVertexAIProfile(500, List.of(MODEL, "gemini-2.5-pro")));
        AssetFilter filter = new AssetFilter(new AssetFilter.Environment("prod", Set.of(new AssetFilter.Asset("llm-google", Set.of(MODEL, "other")))));
        when(this.client.query(any(), any(), any(), any(), any(), any())).thenReturn(response("{}"));

        AssetActivity activity = this.resolver.resolve(multiple, THREE_HOURS, filter).orElseThrow();

        assertEquals(Set.of(MODEL), activity.activity().getFirst().serviceIds());
        verify(this.client).query("organizations/456", FILTER, START.toString(), THREE_HOURS.end().toString(), "3600s", null);
    }

    @Test
    void escapesModelNamesInTheGeneratedFilter() throws Exception
    {
        LLMVertexAIAsset escaped = new LLMVertexAIAsset(this.asset.identity(), this.asset.googleUrl(), PROJECT, null,
            new LLMVertexAIProfile(500, List.of("model\"\\name")));
        when(this.client.query(any(), any(), any(), any(), any(), any())).thenReturn(response("{}"));

        this.resolver.resolve(escaped, THREE_HOURS, null);

        verify(this.client).query(PROJECT, BASE_FILTER + " AND (resource.labels.model_user_id=\"model\\\"\\\\name\")",
            START.toString(), THREE_HOURS.end().toString(), "3600s", null);
    }

    @Test
    void rejectsUsageWithoutModelLabels() throws Exception
    {
        when(this.client.query(any(), any(), any(), any(), any(), any())).thenReturn(response("""
            {"timeSeries":[{"points":[{"value":{"int64Value":"100"}}]}]}
            """));

        assertThrows(WombatActivityException.class, () -> this.resolver.resolve(this.asset, THREE_HOURS, null));
    }

    @Test
    void allModelsWithNoUsageProducesNoActivity() throws Exception
    {
        LLMVertexAIAsset allModels = new LLMVertexAIAsset(this.asset.identity(), this.asset.googleUrl(), PROJECT, null,
            new LLMVertexAIProfile(500, List.of()));
        when(this.client.query(any(), any(), any(), any(), any(), any())).thenReturn(response("{}"));

        assertTrue(this.resolver.resolve(allModels, THREE_HOURS, null).isEmpty());
    }

    @Test
    void resolvesDynamicLocationFromResponseLabels() throws Exception
    {
        when(this.client.query(any(), any(), any(), any(), any(), any())).thenReturn(response("""
            {"timeSeries":[
              {"resource":{"labels":{"model_user_id":"gemini-2.5-flash","location":"europe-west9"}},"points":[
                {"interval":{"endTime":"2026-10-06T21:00:00Z"},"value":{"int64Value":"500"}}]},
              {"resource":{"labels":{"model_user_id":"gemini-2.5-pro","location":"unknown-region"}},"points":[
                {"interval":{"endTime":"2026-10-06T21:00:00Z"},"value":{"int64Value":"300"}}]}
            ]}
            """));

        LLMVertexAIAsset multiModelAsset = new LLMVertexAIAsset(
            this.asset.identity(), this.asset.googleUrl(), PROJECT, null,
            new LLMVertexAIProfile(500, List.of(MODEL, "gemini-2.5-pro"))
        );

        AssetActivity activity = this.resolver.resolve(multiModelAsset, THREE_HOURS, null).orElseThrow();
        LLMActivityData first = (LLMActivityData) activity.activity().getFirst();

        assertEquals(Country.FRANCE, first.services().get(MODEL).location());
        assertEquals(Country.WORLD, first.services().get("gemini-2.5-pro").location());
    }

    private static GoogleQueryResponse response(String json) throws Exception
    {
        return MAPPER.readValue(json, GoogleQueryResponse.class);
    }

    private static GoogleMultiClient multi(GoogleClient client)
    {
        GoogleMultiClient multi = new GoogleMultiClient();
        multi.register("llm-google", client);
        return multi;
    }
}
