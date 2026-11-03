package tech.illuin.wombat.module.llm_anthropic.activity;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.WombatActivityException;
import tech.illuin.wombat.core.activity.commons.AssetActivity;
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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

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
    private static final String H0 = "2026-10-01T00:00:00Z";
    private static final String H1 = "2026-10-01T01:00:00Z";
    private static final String H2 = "2026-10-01T02:00:00Z";
    private static final String H3 = "2026-10-01T03:00:00Z";
    private static final TimeRange THREE_HOURS = new TimeRange(START, START.plusSeconds(3 * 3600));

    private final AnthropicAdminClient client = mock(AnthropicAdminClient.class);
    private final LLMAnthropicAsset asset = new LLMAnthropicAsset(
        AssetIdentity.of("claude", "prod", "Claude"), null, null, "ANTHROPIC_ADMIN_KEY",
        new LLMAnthropicProfile(FRANCE, 500, List.of("claude-opus-5-5"), null, null)
    );
    private final LLMAnthropicActivityResolver resolver = new LLMAnthropicActivityResolver(multi(this.client));

    @Test
    void sumsOutputTokensPerModelAcrossBucketsAndPages() throws WombatActivityException
    {
        this.stubTwoPages();

        LLMActivityData data = (LLMActivityData) this.resolver.resolve(this.asset, THREE_HOURS, null).orElseThrow().activity().getFirst();

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
        this.stubEmpty();

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
    void usesDailyBucketsForADailyResolutionAndHourlyOtherwise() throws WombatActivityException
    {
        this.stubEmpty();

        this.resolver.resolve(this.asset, new TimeRange(START, START.plus(Duration.ofDays(2)), Duration.ofDays(1)), null);
        this.resolver.resolve(this.asset, new TimeRange(START, START.plus(Duration.ofDays(2)), Duration.ofHours(6)), null);

        verify(this.client).usageReport(any(), any(), eq("1d"), eq(31), any(), any(), any(), any());
        verify(this.client).usageReport(any(), any(), eq("1h"), eq(168), any(), any(), any(), any());
    }

    @Test
    void producesOneActivityPerResolutionSlotFromASingleReport() throws WombatActivityException
    {
        this.stubTwoPages();

        AssetActivity activity = this.resolver.resolve(this.asset, new TimeRange(START, START.plusSeconds(3 * 3600), Duration.ofHours(1)), null).orElseThrow();

        assertEquals(this.asset.identity(), activity.assetIdentity());
        assertEquals(3, activity.activity().size());
        LLMActivityData h0 = (LLMActivityData) activity.activity().get(0);
        assertEquals(new TimeRange(START, START.plusSeconds(3600), Duration.ofHours(1)), h0.range());
        assertEquals(1000, h0.services().get("claude-opus-5-5").outputTokenCount());
        assertEquals(250, h0.services().get("claude-haiku-5-5").outputTokenCount());
        assertEquals(Set.of("claude-opus-5-5"), ((LLMActivityData) activity.activity().get(1)).services().keySet());
        assertEquals(1000, ((LLMActivityData) activity.activity().get(2)).services().get("claude-opus-5-5").outputTokenCount());
        verify(this.client, times(2)).usageReport(any(), any(), eq("1h"), anyInt(), any(), any(), any(), any());
    }

    @Test
    void returnsEmptyWhenThereIsNoUsage() throws WombatActivityException
    {
        when(this.client.usageReport(any(), any(), any(), anyInt(), any(), any(), any(), any())).thenReturn(new AnthropicUsageReport(List.of(new Bucket(H0, H1, List.of())), false, null));

        assertTrue(this.resolver.resolve(this.asset, new TimeRange(START, START.plusSeconds(60)), null).isEmpty());
    }

    private void stubTwoPages()
    {
        when(this.client.usageReport(any(), any(), any(), anyInt(), any(), any(), any(), isNull())).thenReturn(new AnthropicUsageReport(
            List.of(
                new Bucket(H0, H1, List.of(new Result("claude-opus-5-5", 10, 0, 1000), new Result("claude-haiku-5-5", 10, 0, 250))),
                new Bucket(H1, H2, List.of(new Result("claude-opus-5-5", 10, 0, 500)))
            ),
            true, "page_2"
        ));
        when(this.client.usageReport(any(), any(), any(), anyInt(), any(), any(), any(), eq("page_2"))).thenReturn(new AnthropicUsageReport(
            List.of(new Bucket(H2, H3, List.of(new Result("claude-opus-5-5", 10, 0, 1000)))),
            false, null
        ));
    }

    private void stubEmpty()
    {
        when(this.client.usageReport(any(), any(), any(), anyInt(), any(), any(), any(), any())).thenReturn(new AnthropicUsageReport(List.of(), false, null));
    }

    private static AnthropicMultiClient multi(AnthropicAdminClient client)
    {
        AnthropicMultiClient multi = new AnthropicMultiClient();
        multi.register("claude", client);
        return multi;
    }
}
