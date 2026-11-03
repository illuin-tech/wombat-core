package tech.illuin.wombat.module.llm_anthropic.connector;

import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.Environment;
import tech.illuin.wombat.core.context.ResolvedContext;
import tech.illuin.wombat.core.secret.EnvironmentSecretResolver;
import tech.illuin.wombat.module.llm_anthropic.LLMAnthropicAsset;
import tech.illuin.wombat.module.llm_anthropic.LLMAnthropicProfile;
import tech.illuin.wombat.module.llm_anthropic.connector.model.AnthropicUsageReport;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static tech.illuin.wombat.core.activity.commons.Country.FRANCE;

class AnthropicMultiClientConfigTest
{
    private static final String BODY = """
        {
          "data": [{
            "starting_at": "2026-10-01T00:00:00Z",
            "ending_at": "2026-10-01T00:01:00Z",
            "results": [{
              "model": "claude-opus-5-5",
              "uncached_input_tokens": 1500,
              "cache_creation": {"ephemeral_1h_input_tokens": 0, "ephemeral_5m_input_tokens": 0},
              "cache_read_input_tokens": 200,
              "output_tokens": 500,
              "server_tool_use": {"web_search_requests": 0},
              "workspace_id": null
            }]
          }],
          "has_more": false,
          "next_page": null
        }
        """;

    @Test
    void sendsAdminHeadersAndExpandsArrayParameters()
    {
        AtomicReference<Request> captured = new AtomicReference<>();
        feign.Client http = (request, options) -> {
            captured.set(request);
            return Response.builder()
                .request(request)
                .status(200)
                .headers(Map.of())
                .body(BODY, StandardCharsets.UTF_8)
                .build();
        };

        LLMAnthropicAsset asset = new LLMAnthropicAsset(
            AssetIdentity.of("claude", "prod", "Claude"), null, null, "ANTHROPIC_ADMIN_KEY",
            new LLMAnthropicProfile(FRANCE, 500, null, null, null)
        );
        ResolvedContext context = new ResolvedContext(
            List.of(new Environment("prod", List.of(asset))),
            new EnvironmentSecretResolver(Map.of("ANTHROPIC_ADMIN_KEY", "sk-ant-admin-test")::get)
        );
        AnthropicAdminClient client = AnthropicMultiClientConfig.create(context, http).get("claude").orElseThrow();

        AnthropicUsageReport report = client.usageReport(
            "2026-10-01T00:00:00Z", "2026-10-01T01:00:00Z", "1m", 1440,
            List.of("claude-opus-5-5", "claude-haiku-5-5"), null, null, null
        );

        Request request = captured.get();
        String url = java.net.URLDecoder.decode(request.url(), StandardCharsets.UTF_8);
        assertTrue(url.startsWith("https://api.anthropic.com/v1/organizations/usage_report/messages?"), url);
        assertTrue(url.contains("starting_at=2026-10-01T00:00:00Z"), url);
        assertTrue(url.contains("group_by[]=model"), url);
        assertTrue(url.contains("ending_at=2026-10-01T01:00:00Z"), url);
        assertTrue(url.contains("bucket_width=1m"), url);
        assertTrue(url.contains("limit=1440"), url);
        assertTrue(url.contains("models[]=claude-opus-5-5&models[]=claude-haiku-5-5"), url);
        // null filters and cursor are left out of the query entirely
        assertFalse(url.contains("workspace_ids"), url);
        assertFalse(url.contains("api_key_ids"), url);
        assertFalse(url.contains("page="), url);
        assertEquals(List.of("sk-ant-admin-test"), List.copyOf(request.headers().get("x-api-key")));
        assertEquals(List.of("2023-06-01"), List.copyOf(request.headers().get("anthropic-version")));

        assertEquals(500, report.data().getFirst().results().getFirst().outputTokens());
        assertFalse(report.hasMore());
    }
}
