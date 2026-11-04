package tech.illuin.wombat.module.llm_static.activity;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.commons.AssetActivity;
import tech.illuin.wombat.core.activity.commons.AssetFilter;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.activity.llm.LLMActivityData;
import tech.illuin.wombat.core.activity.llm.LLMServiceActivity;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.module.llm_static.LLMStaticAsset;
import tech.illuin.wombat.module.llm_static.LLMStaticProfile;
import tech.illuin.wombat.module.llm_static.scenario.BatchActivityScenario;
import tech.illuin.wombat.module.llm_static.scenario.ConstantScenario;
import tech.illuin.wombat.module.llm_static.scenario.UserActivityScenario;
import tech.illuin.wombat.module.llm_static.scenario.tools.ApplicabilityFilter;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.time.DayOfWeek.*;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.Month.JANUARY;
import static org.junit.jupiter.api.Assertions.*;

class LLMStaticActivityResolverTest
{
    private final LLMStaticActivityResolver resolver = new LLMStaticActivityResolver();

    @Test
    void resolve_multiModelStaticProfile_computesActivityForEachModel()
    {
        LLMStaticProfile.ModelConfig m1 = new LLMStaticProfile.ModelConfig(
            LLMProvider.mistralai,
            "mistral-large-latest",
            Country.FRANCE,
            List.of(new ConstantScenario(500, 31_536_000, null)) // 1 req/sec -> 31536000 req/year
        );
        LLMStaticProfile.ModelConfig m2 = new LLMStaticProfile.ModelConfig(
            LLMProvider.openai,
            "gpt-4o",
            Country.US,
            List.of(new ConstantScenario(1000, 63_072_000, null)) // 2 req/sec
        );

        LLMStaticProfile profile = new LLMStaticProfile(List.of(m1, m2));
        LLMStaticAsset asset = new LLMStaticAsset(AssetIdentity.of("llm-asset", "test-env", "Test Static Asset"), profile);

        // 100 seconds range
        TimeRange range = new TimeRange(Instant.ofEpochSecond(0), Instant.ofEpochSecond(100));

        Optional<AssetActivity> activityOpt = this.resolver.resolve(asset, range, null);
        assertTrue(activityOpt.isPresent());
        assertInstanceOf(LLMActivityData.class, activityOpt.get().activity().getFirst());

        AssetActivity activity = activityOpt.get();
        LLMActivityData llmActivity = (LLMActivityData) activity.activity().getFirst();
        assertEquals(2, llmActivity.services().size());

        // 100 seconds out of 365 days (31536000s in year)
        LLMServiceActivity a1 = llmActivity.services().get("mistral-large-latest");
        assertNotNull(a1);
        assertEquals(LLMProvider.mistralai, a1.provider());
        assertEquals("mistral-large-latest", a1.model());
        assertEquals(100, (int) a1.requestCount());
        assertEquals(50_000L, a1.outputTokenCount()); // 500 * 100

        LLMServiceActivity a2 = llmActivity.services().get("gpt-4o");
        assertNotNull(a2);
        assertEquals(LLMProvider.openai, a2.provider());
        assertEquals("gpt-4o", a2.model());
        assertEquals(200, (int) a2.requestCount());
        assertEquals(200_000L, a2.outputTokenCount()); // 1000 * 200

        assertEquals(250_000L, llmActivity.outputTokenCount());
        assertEquals(300, llmActivity.requestCount());
    }

    @Test
    void resolvesConstantScenarioAcrossTimeSlots()
    {
        LLMStaticAsset asset = new LLMStaticAsset(
            new AssetIdentity("llm-static-1", "env-test", "Static LLM 1"),
            new LLMStaticProfile(
                LLMProvider.openai,
                "gpt-4o",
                Country.FRANCE,
                List.of(new ConstantScenario(200, 31_536_000, null)) // ~1 req/sec -> 3600 req/hour
            )
        );

        Instant start = Instant.parse("2026-11-01T00:00:00Z");
        Instant end = Instant.parse("2026-11-01T02:00:00Z");
        TimeRange range = new TimeRange(start, end, Duration.ofHours(1));

        Optional<AssetActivity> result = this.resolver.resolve(asset, range, null);
        assertTrue(result.isPresent());

        AssetActivity activity = result.get();
        assertEquals(asset.identity(), activity.assetIdentity());
        assertEquals(2, activity.activity().size());

        for (var actData : activity.activity())
        {
            assertInstanceOf(LLMActivityData.class, actData);
            LLMActivityData llmData = (LLMActivityData) actData;
            assertEquals(ActivityRegime.MODELED, llmData.regime());
            assertEquals(3600, llmData.requestCount());
            assertEquals(720_000L, llmData.outputTokenCount()); // 200 * 3600
        }
    }

    @Test
    void resolvesSuperposedScenariosAcrossTimeSlots()
    {
        // 1 req/sec = 31536000 req/year -> 3600 req/hour
        // Baseline: 3153600 req/year -> 360 req/hour
        // User Activity: peak 31536000 req/year -> 3600 req/hour (09:00 - 18:00, sinusoidal)
        // Batch Activity: peak 63072000 req/year -> 7200 req/hour (02:00 - 04:00, duration 2h)
        int userTotalRequests = (int) (31_536_000 * (9.0 / 24.0) * (2.0 / Math.PI));
        int batchTotalRequests = (int) (63_072_000 * (2.0 / 24.0) * 1.0);

        LLMStaticAsset asset = new LLMStaticAsset(
            new AssetIdentity("llm-static-2", "env-test", "Static LLM 2"),
            new LLMStaticProfile(
                LLMProvider.openai,
                "gpt-4o",
                Country.FRANCE,
                List.of(
                    new ConstantScenario(100, 3_153_600, null),
                    new UserActivityScenario(200, userTotalRequests, LocalTime.of(9, 0), LocalTime.of(18, 0), UserActivityScenario.CurveShape.SINUSOIDAL, 0.0, null),
                    new BatchActivityScenario(500, batchTotalRequests, LocalTime.of(2, 0), Duration.ofHours(2), 0.0, BatchActivityScenario.SmoothingMode.NONE, null)
                )
            )
        );

        Instant start = Instant.parse("2026-11-01T00:00:00Z");
        Instant end = Instant.parse("2026-11-02T00:00:00Z");
        TimeRange range = new TimeRange(start, end, Duration.ofHours(1));

        Optional<AssetActivity> result = this.resolver.resolve(asset, range, null);
        assertTrue(result.isPresent());

        List<LLMActivityData> slots = result.get().activity().stream()
            .map(a -> (LLMActivityData) a)
            .toList();

        assertEquals(24, slots.size());

        // Slot 2: 02:00-03:00 -> batch active (7200 req, 500 tokens/req = 3600000 tokens)
        LLMActivityData slot2 = slots.get(2);
        assertEquals(7200.0, slot2.requestCount());
        assertEquals(3_600_000L, slot2.outputTokenCount());

        // Slot 13: 13:00-14:00 (midpoint 13:30) -> peak user activity (3600 req, 200 tokens/req = 720000 tokens)
        LLMActivityData slot13 = slots.get(13);
        assertEquals(3600.0, slot13.requestCount());
        assertEquals(720_000L, slot13.outputTokenCount());

        // Slot 22: 22:00-23:00 -> constant baseline (360 req, 100 tokens/req = 36000 tokens)
        LLMActivityData slot22 = slots.get(22);
        assertEquals(360.0, slot22.requestCount());
        assertEquals(36_000L, slot22.outputTokenCount());
    }

    @Test
    void resolvesFilteredModelsWithAssetFilter()
    {
        LLMStaticProfile.ModelConfig m1 = new LLMStaticProfile.ModelConfig(
            LLMProvider.mistralai,
            "mistral-large-latest",
            Country.FRANCE,
            List.of(new ConstantScenario(500, 31_536_000, null))
        );
        LLMStaticProfile.ModelConfig m2 = new LLMStaticProfile.ModelConfig(
            LLMProvider.openai,
            "gpt-4o",
            Country.US,
            List.of(new ConstantScenario(1000, 63_072_000, null))
        );

        LLMStaticProfile profile = new LLMStaticProfile(List.of(m1, m2));
        LLMStaticAsset asset = new LLMStaticAsset(AssetIdentity.of("llm-asset", "test-env", "Test Static Asset"), profile);

        TimeRange range = new TimeRange(Instant.ofEpochSecond(0), Instant.ofEpochSecond(100));
        AssetFilter filter = new AssetFilter(new AssetFilter.Environment("test-env", Set.of(new AssetFilter.Asset("llm-asset", Set.of("mistral-large-latest")))));

        Optional<AssetActivity> activityOpt = this.resolver.resolve(asset, range, filter);
        assertTrue(activityOpt.isPresent());

        LLMActivityData llmActivity = (LLMActivityData) activityOpt.get().activity().getFirst();
        assertEquals(1, llmActivity.services().size());
        assertTrue(llmActivity.services().containsKey("mistral-large-latest"));
        assertFalse(llmActivity.services().containsKey("gpt-4o"));
        assertEquals(100, (int) llmActivity.requestCount());
        assertEquals(50000L, llmActivity.outputTokenCount());
    }

    @Test
    void resolvesDailyResolutionWithHourlySampling()
    {
        int userTotalRequests = (int) (31_536_000 * (9.0 / 24.0) * (2.0 / Math.PI));

        LLMStaticAsset asset = new LLMStaticAsset(
            new AssetIdentity("llm-static-daily", "env-test", "Daily Static LLM"),
            new LLMStaticProfile(
                LLMProvider.openai,
                "gpt-4o",
                Country.FRANCE,
                List.of(
                    new ConstantScenario(100, 3_153_600, null),
                    new UserActivityScenario(
                        200,
                        userTotalRequests,
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0),
                        UserActivityScenario.CurveShape.SINUSOIDAL,
                        0.0
                    )
                )
            )
        );

        Instant start = Instant.parse("2026-11-01T00:00:00Z");
        Instant end = Instant.parse("2026-11-02T00:00:00Z");
        TimeRange range = new TimeRange(start, end);

        Optional<AssetActivity> result = this.resolver.resolve(asset, range, null);
        assertTrue(result.isPresent());

        AssetActivity activity = result.get();
        assertEquals(1, activity.activity().size());

        LLMActivityData slot = (LLMActivityData) activity.activity().getFirst();
        assertEquals(ActivityRegime.MODELED, slot.regime());
        assertEquals(range, slot.range());

        // 24 sub-slots evaluated at hourly resolution:
        // 15 constant slots (100 tokens/req), 9 user sinusoidal slots (200 tokens/req)
        // Daily requestCount = 26,132
        // Mean tokens/req = (100*15 + 200*9) / 24 = 137.5 tokens/req
        // Daily outputTokenCount = (long)(137.5 * 26,132) = 3,593,150
        assertEquals(26_132, slot.requestCount());
        assertEquals(3_593_150L, slot.outputTokenCount());

        LLMServiceActivity gpt4o = slot.services().get("gpt-4o");
        assertNotNull(gpt4o);
        assertEquals(26_132, gpt4o.requestCount());
        assertEquals(3_593_150L, gpt4o.outputTokenCount());
    }

    @Test
    void resolvesSevenDayAggregateBatchActivityScenario()
    {
        LLMStaticAsset asset = new LLMStaticAsset(
            new AssetIdentity("llm-batch-7d", "env-test", "Batch 7D Asset"),
            new LLMStaticProfile(
                LLMProvider.openai,
                "gpt-4o",
                Country.FRANCE,
                List.of(
                    new BatchActivityScenario(1000, 100_000, LocalTime.of(2, 0), Duration.ofHours(2), 0.0, BatchActivityScenario.SmoothingMode.NONE)
                )
            )
        );

        Instant start = Instant.parse("2026-11-01T00:00:00Z");
        Instant end = Instant.parse("2026-11-08T00:00:00Z");
        TimeRange range = new TimeRange(start, end, Duration.ofDays(7));

        Optional<AssetActivity> result = this.resolver.resolve(asset, range, null);
        assertTrue(result.isPresent());

        AssetActivity activity = result.get();
        assertEquals(1, activity.activity().size());

        LLMActivityData slot = (LLMActivityData) activity.activity().getFirst();
        assertEquals(ActivityRegime.MODELED, slot.regime());
        assertEquals(1918, slot.requestCount());
        assertEquals(1_918_000L, slot.outputTokenCount());
    }

    @Test
    void resolvesFullYearSummationForUserActivityScenario()
    {
        int targetAnnualRequests = 100_000;
        LLMStaticAsset asset = new LLMStaticAsset(
            new AssetIdentity("llm-user-year", "env-test", "User Year Asset"),
            new LLMStaticProfile(
                LLMProvider.openai,
                "gpt-4o",
                Country.FRANCE,
                List.of(
                    new UserActivityScenario(100, targetAnnualRequests, LocalTime.of(9, 0), LocalTime.of(18, 0), UserActivityScenario.CurveShape.BELL, 0.0)
                )
            )
        );

        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2027-01-01T00:00:00Z");
        TimeRange range = new TimeRange(start, end, Duration.ofHours(1));

        Optional<AssetActivity> result = this.resolver.resolve(asset, range, null);
        assertTrue(result.isPresent());

        long totalRequests = result.get().activity().stream()
            .mapToLong(a -> (long) ((LLMActivityData) a).requestCount())
            .sum();

        // 8,760 1-hour slots integrated over standard year should be within 0.5% of target annual volume
        assertEquals(targetAnnualRequests, (double) totalRequests, targetAnnualRequests * 0.005);
    }

    @Test
    void resolvesFullYearSummationForBatchActivityScenario()
    {
        int targetAnnualRequests = 50_000;
        LLMStaticAsset asset = new LLMStaticAsset(
            new AssetIdentity("llm-batch-year", "env-test", "Batch Year Asset"),
            new LLMStaticProfile(
                LLMProvider.openai,
                "gpt-4o",
                Country.FRANCE,
                List.of(
                    new BatchActivityScenario(200, targetAnnualRequests, LocalTime.of(2, 0), Duration.ofHours(10), 0.0, BatchActivityScenario.SmoothingMode.SMOOTH_STEP)
                )
            )
        );

        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2027-01-01T00:00:00Z");
        TimeRange range = new TimeRange(start, end, Duration.ofHours(1));

        Optional<AssetActivity> result = this.resolver.resolve(asset, range, null);
        assertTrue(result.isPresent());

        long totalRequests = result.get().activity().stream()
            .mapToLong(a -> (long) ((LLMActivityData) a).requestCount())
            .sum();

        assertEquals(targetAnnualRequests, (double) totalRequests, targetAnnualRequests * 0.01);
    }

    @Test
    void resolvesFullYearSummationWithMonthApplicabilityFilter()
    {
        int targetAnnualRequests = 100_000;
        ApplicabilityFilter janFilter = new ApplicabilityFilter(Set.of(), Set.of(JANUARY));

        LLMStaticAsset asset = new LLMStaticAsset(
            new AssetIdentity("llm-jan-asset", "env-test", "Jan Only Asset"),
            new LLMStaticProfile(
                LLMProvider.openai,
                "gpt-4o",
                Country.FRANCE,
                List.of(
                    new UserActivityScenario(100, targetAnnualRequests, LocalTime.of(9, 0), LocalTime.of(18, 0), UserActivityScenario.CurveShape.BELL, 0.0, janFilter)
                )
            )
        );

        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2027-01-01T00:00:00Z");
        TimeRange range = new TimeRange(start, end, Duration.ofHours(1));

        Optional<AssetActivity> result = this.resolver.resolve(asset, range, null);
        assertTrue(result.isPresent());

        List<tech.illuin.wombat.core.activity.commons.ActivityData> activities = result.get().activity();
        long totalRequests = 0;
        long nonJanuaryRequests = 0;

        Instant feb1 = Instant.parse("2026-02-01T00:00:00Z");
        for (var act : activities)
        {
            LLMActivityData llmAct = (LLMActivityData) act;
            totalRequests += (long) llmAct.requestCount();
            if (!act.range().start().isBefore(feb1))
                nonJanuaryRequests += (long) llmAct.requestCount();
        }

        assertEquals(0, nonJanuaryRequests);
        assertEquals(targetAnnualRequests, (double) totalRequests, targetAnnualRequests * 0.005);
    }

    @Test
    void resolvesFullYearSummationWithWeekdayApplicabilityFilter()
    {
        int targetAnnualRequests = 100_000;
        ApplicabilityFilter weekdayFilter = new ApplicabilityFilter(
            Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY),
            Set.of()
        );

        LLMStaticAsset asset = new LLMStaticAsset(
            new AssetIdentity("llm-weekday-asset", "env-test", "Weekday Asset"),
            new LLMStaticProfile(
                LLMProvider.openai,
                "gpt-4o",
                Country.FRANCE,
                List.of(
                    new UserActivityScenario(100, targetAnnualRequests, LocalTime.of(9, 0), LocalTime.of(18, 0), UserActivityScenario.CurveShape.BELL, 0.0, weekdayFilter)
                )
            )
        );

        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2027-01-01T00:00:00Z");
        TimeRange range = new TimeRange(start, end, Duration.ofHours(1));

        Optional<AssetActivity> result = this.resolver.resolve(asset, range, null);
        assertTrue(result.isPresent());

        List<tech.illuin.wombat.core.activity.commons.ActivityData> activities = result.get().activity();
        long totalRequests = 0;
        long weekendRequests = 0;

        for (var act : activities)
        {
            LLMActivityData llmAct = (LLMActivityData) act;
            totalRequests += (long) llmAct.requestCount();
            java.time.DayOfWeek dow = act.range().start().atZone(ZoneOffset.UTC).getDayOfWeek();
            if (dow == java.time.DayOfWeek.SATURDAY || dow == java.time.DayOfWeek.SUNDAY)
                weekendRequests += (long) llmAct.requestCount();
        }

        assertEquals(0, weekendRequests);
        assertEquals(targetAnnualRequests, (double) totalRequests, targetAnnualRequests * 0.005);
    }
}
