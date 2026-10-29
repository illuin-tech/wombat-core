package tech.illuin.wombat.module.llm_anthropic.activity;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.WombatActivityException;
import tech.illuin.wombat.core.activity.WombatActivityResolver.ActivityResolverType;
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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
    void isAnExternalApiResolver()
    {
        assertEquals(ActivityResolverType.EXTERNAL_API, this.resolver.activityResolverType());
    }

    @Test
    void sumsOutputTokensPerModelAcrossBucketsAndPages() throws WombatActivityException
    {
        this.stubTwoPages();

        LLMActivityData data = (LLMActivityData) this.resolver.resolve(this.asset, THREE_HOURS, null).orElseThrow();

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
    void usesHourlyBucketsUpToSevenDaysAndDailyBeyond() throws WombatActivityException
    {
        this.stubEmpty();

        this.resolver.resolve(this.asset, new TimeRange(START, START.plus(TimeRange.HOURLY_MAX_SPAN)), null);
        this.resolver.resolve(this.asset, new TimeRange(START, START.plus(Duration.ofDays(8))), null);

        verify(this.client).usageReport(any(), any(), eq("1h"), eq(168), any(), any(), any(), any());
        verify(this.client).usageReport(any(), any(), eq("1d"), eq(31), any(), any(), any(), any());
    }

    @Test
    void returnsNoServicesWhenThereIsNoUsage() throws WombatActivityException
    {
        when(this.client.usageReport(any(), any(), any(), anyInt(), any(), any(), any(), any())).thenReturn(new AnthropicUsageReport(List.of(new Bucket(H0, H1, List.of())), false, null));

        ActivityData data = this.resolver.resolve(this.asset, new TimeRange(START, START.plusSeconds(60)), null).orElseThrow();

        assertTrue(((LLMActivityData) data).services().isEmpty());
    }

    @Test
    void activityPerBucket_returnsOneCurvePerSelectedModel() throws WombatActivityException
    {
        this.stubTwoPages();

        assertEquals(
            Map.of(
                "claude-opus-5-5", Map.of(ms(H0), 1000.0, ms(H1), 500.0, ms(H2), 1000.0),
                "claude-haiku-5-5", Map.of(ms(H0), 250.0)
            ),
            this.resolver.activityPerBucket(this.asset, THREE_HOURS, Duration.ofHours(1).toMillis(), Set.of())
        );
        assertEquals(
            Map.of("claude-haiku-5-5", Map.of(ms(H0), 250.0)),
            this.resolver.activityPerBucket(this.asset, THREE_HOURS, Duration.ofHours(1).toMillis(), Set.of("claude-haiku-5-5"))
        );
    }

    @Test
    void activityPerBucket_usesDailyBucketsForADailyStep() throws WombatActivityException
    {
        this.stubEmpty();

        this.resolver.activityPerBucket(this.asset, new TimeRange(START, START.plus(Duration.ofDays(30))), Duration.ofDays(1).toMillis(), Set.of());

        verify(this.client).usageReport(any(), any(), eq("1d"), eq(31), any(), any(), any(), any());
    }

    @Test
    void activityPerBucket_rejectsStepsFinerThanAnHour()
    {
        assertThrows(IllegalArgumentException.class, () -> this.resolver.activityPerBucket(this.asset, THREE_HOURS, Duration.ofMinutes(5).toMillis(), Set.of()));
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

    private static long ms(String instant)
    {
        return Instant.parse(instant).toEpochMilli();
    }

    private static AnthropicMultiClient multi(AnthropicAdminClient client)
    {
        AnthropicMultiClient multi = new AnthropicMultiClient();
        multi.register("claude", client);
        return multi;
    }
}
