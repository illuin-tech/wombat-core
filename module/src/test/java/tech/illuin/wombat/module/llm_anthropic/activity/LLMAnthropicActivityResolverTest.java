package tech.illuin.wombat.module.llm_anthropic.activity;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.WombatActivityException;
import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.activity.llm.LLMActivityData;
import tech.illuin.wombat.core.activity.llm.LLMServiceActivity;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.module.llm_anthropic.LLMAnthropicAsset;
import tech.illuin.wombat.module.llm_anthropic.LLMAnthropicProfile;
import tech.illuin.wombat.module.llm_anthropic.connector.AnthropicAdminClient;
import tech.illuin.wombat.module.llm_anthropic.connector.AnthropicMultiClient;
import tech.illuin.wombat.module.llm_anthropic.connector.model.AnthropicUsageReport;
import tech.illuin.wombat.module.llm_anthropic.connector.model.AnthropicUsageReport.Bucket;
import tech.illuin.wombat.module.llm_anthropic.connector.model.AnthropicUsageReport.Result;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static tech.illuin.wombat.core.activity.commons.Country.FRANCE;

class LLMAnthropicActivityResolverTest
{
    private static final Instant START = Instant.parse("2026-10-01T00:00:00Z");

    private final AnthropicAdminClient client = mock(AnthropicAdminClient.class);
    private final LLMAnthropicAsset asset = new LLMAnthropicAsset(
        AssetIdentity.of("claude", "prod", "Claude"), null, null, "ANTHROPIC_ADMIN_KEY",
        new LLMAnthropicProfile(FRANCE, 500, List.of("claude-opus-5-5"), null, null)
    );
    private final LLMAnthropicActivityResolver resolver = new LLMAnthropicActivityResolver(multi(this.client));

    @Test
    void sumsOutputTokensPerModelAcrossBucketsAndPages() throws WombatActivityException
    {
        when(this.client.usageReport(any(), any(), any(), anyInt(), any(), any(), any(), isNull())).thenReturn(new AnthropicUsageReport(
            List.of(
                new Bucket("a", "b", List.of(new Result("claude-opus-5-5", 10, 0, 1000), new Result("claude-haiku-5-5", 10, 0, 250))),
                new Bucket("b", "c", List.of(new Result("claude-opus-5-5", 10, 0, 500)))
            ),
            true, "page_2"
        ));
        when(this.client.usageReport(any(), any(), any(), anyInt(), any(), any(), any(), eq("page_2"))).thenReturn(new AnthropicUsageReport(
            List.of(new Bucket("c", "d", List.of(new Result("claude-opus-5-5", 10, 0, 1000)))),
            false, null
        ));

        LLMActivityData data = (LLMActivityData) this.resolver.resolve(this.asset, new TimeRange(START, START.plusSeconds(3600)), null).orElseThrow();

        LLMServiceActivity opus = data.services().get("claude-opus-5-5");
        assertEquals(LLMProvider.anthropic, opus.provider());
        assertEquals(FRANCE, opus.location());
        assertEquals(2500, opus.outputTokenCount());
        assertEquals(5.0, opus.requestCount());
        assertEquals(250, data.services().get("claude-haiku-5-5").outputTokenCount());
        verify(this.client, times(2)).usageReport(any(), any(), any(), anyInt(), any(), any(), any(), any());
    }

    @Test
    void sendsTheRangeGroupingAndProfileFilters() throws WombatActivityException
    {
        when(this.client.usageReport(any(), any(), any(), anyInt(), any(), any(), any(), any())).thenReturn(new AnthropicUsageReport(List.of(), false, null));

        this.resolver.resolve(this.asset, new TimeRange(START, START.plusSeconds(3600)), null);

        verify(this.client).usageReport(
            "2026-10-01T00:00:00Z",
            "2026-10-01T01:00:00Z",
            "1h",
            168,
            List.of("claude-opus-5-5"),
            null,
            null,
            null
        );
    }

    @Test
    void returnsNoServicesWhenThereIsNoUsage() throws WombatActivityException
    {
        when(this.client.usageReport(any(), any(), any(), anyInt(), any(), any(), any(), any())).thenReturn(new AnthropicUsageReport(List.of(new Bucket("a", "b", List.of())), false, null));

        ActivityData data = this.resolver.resolve(this.asset, new TimeRange(START, START.plusSeconds(60)), null).orElseThrow();

        assertTrue(((LLMActivityData) data).services().isEmpty());
    }

    private static AnthropicMultiClient multi(AnthropicAdminClient client)
    {
        AnthropicMultiClient multi = new AnthropicMultiClient();
        multi.register("claude", client);
        return multi;
    }
}
