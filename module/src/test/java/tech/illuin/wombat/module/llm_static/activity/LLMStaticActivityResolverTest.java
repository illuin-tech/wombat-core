package tech.illuin.wombat.module.llm_static.activity;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.module.llm_static.LLMStaticModule;
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
import tech.illuin.wombat.module.llm_static.scenario.Scenario;
import tech.illuin.wombat.module.llm_static.scenario.UserActivityScenario;
import tech.illuin.wombat.module.llm_static.scenario.tools.ApplicabilityFilter;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.time.DayOfWeek.*;
import static org.junit.jupiter.api.Assertions.*;

class LLMStaticActivityResolverTest
{
    private final LLMStaticActivityResolver resolver = new LLMStaticActivityResolver();

    /* Where the resolver is meant to be exact: only floating-point error */
    private static final double EXACT = 1e-9;
    private static final Instant YEAR_2026 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant YEAR_2027 = Instant.parse("2027-01-01T00:00:00Z");
    private static final List<Duration> RESOLUTIONS = List.of(Duration.ofHours(1), Duration.ofDays(1), Duration.ofDays(5));
    private static final Set<DayOfWeek> WEEKDAYS = Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY);

    @Test
    void resolve_multiModelStaticProfile_computesActivityForEachModel()
    {
        /* 1 and 2 requests a second */
        LLMStaticProfile.ModelConfig m1 = model(LLMProvider.mistralai, "mistral-large-latest", 31_536_000, 500, new ConstantScenario());
        LLMStaticProfile.ModelConfig m2 = model(LLMProvider.openai, "gpt-4o", 63_072_000, 1000, new ConstantScenario());

        Optional<AssetActivity> activityOpt = this.resolver.resolve(asset("llm-asset", m1, m2), new TimeRange(Instant.ofEpochSecond(0), Instant.ofEpochSecond(100)), null);
        assertTrue(activityOpt.isPresent());
        LLMActivityData llmActivity = assertInstanceOf(LLMActivityData.class, activityOpt.get().activity().getFirst());
        assertEquals(2, llmActivity.services().size());

        LLMServiceActivity a1 = llmActivity.services().get("mistral-large-latest");
        assertEquals(LLMProvider.mistralai, a1.provider());
        assertEquals("mistral-large-latest", a1.model());
        assertEquals(100.0, a1.requestCount(), 100.0 * EXACT);
        assertEquals(50_000L, a1.outputTokenCount());

        LLMServiceActivity a2 = llmActivity.services().get("gpt-4o");
        assertEquals(LLMProvider.openai, a2.provider());
        assertEquals("gpt-4o", a2.model());
        assertEquals(200.0, a2.requestCount(), 200.0 * EXACT);
        assertEquals(200_000L, a2.outputTokenCount());
    }

    @Test
    void resolvesFilteredModelsWithAssetFilter()
    {
        LLMStaticProfile.ModelConfig m1 = model(LLMProvider.mistralai, "mistral-large-latest", 31_536_000, 500, new ConstantScenario());
        LLMStaticProfile.ModelConfig m2 = model(LLMProvider.openai, "gpt-4o", 63_072_000, 1000, new ConstantScenario());
        AssetFilter filter = new AssetFilter(new AssetFilter.Environment("env-test", Set.of(new AssetFilter.Asset("llm-asset", Set.of("mistral-large-latest")))));

        LLMActivityData llmActivity = (LLMActivityData) this.resolver.resolve(asset("llm-asset", m1, m2), new TimeRange(Instant.ofEpochSecond(0), Instant.ofEpochSecond(100)), filter)
            .orElseThrow().activity().getFirst();

        assertEquals(Set.of("mistral-large-latest"), llmActivity.services().keySet());
        assertEquals(100.0, llmActivity.requestCount(), 100.0 * EXACT);
        assertEquals(50_000L, llmActivity.outputTokenCount());
    }

    @Test
    void resolvesConstantTrafficAcrossTimeSlots()
    {
        LLMStaticAsset asset = asset("llm-constant", model(31_536_000, 200, new ConstantScenario()));
        Instant start = Instant.parse("2026-11-01T00:00:00Z");

        List<LLMActivityData> slots = this.slots(asset, new TimeRange(start, start.plus(Duration.ofHours(2)), Duration.ofHours(1)));

        assertEquals(2, slots.size());
        for (LLMActivityData slot : slots)
        {
            assertEquals(ActivityRegime.MODELED, slot.regime());
            assertEquals(3600.0, slot.requestCount(), 3600.0 * EXACT);
            assertEquals(720_000L, slot.outputTokenCount());
        }
    }

    @Test
    void holdsTheAnnualVolumeOverA365DayYear()
    {
        LLMStaticAsset asset = asset("llm-volume", model(1_000_000, 800, new ConstantScenario()));

        for (Duration resolution : RESOLUTIONS)
            assertTotals(1_000_000, 800, this.totals(asset, new TimeRange(YEAR_2026, YEAR_2027, resolution)), "at " + resolution);
        assertTotals(1_000_000, 800, this.totals(asset, new TimeRange(YEAR_2026, YEAR_2027)), "as one slot");
    }

    /** Requests stay fractional: a volume of under one request an hour adds up to its year. */
    @Test
    void holdsALowAnnualVolumeAtHourlyResolution()
    {
        LLMStaticAsset asset = asset("llm-low-volume", model(1_000, 500, new UserActivityScenario(1.0, LocalTime.of(9, 0), LocalTime.of(18, 0), UserActivityScenario.CurveShape.BELL)));

        assertTotals(1_000, 500, this.totals(asset, new TimeRange(YEAR_2026, YEAR_2027, Duration.ofHours(1))), "at PT1H");
    }

    /** A year holds 365 days of volume: a leap year one day more, as the volume is a rate. */
    @Test
    void holdsOneMoreDayOverALeapYear()
    {
        LLMStaticAsset asset = asset("llm-leap-year", model(1_000_000, 800, new ConstantScenario()));
        Totals totals = this.totals(asset, new TimeRange(Instant.parse("2028-01-01T00:00:00Z"), Instant.parse("2029-01-01T00:00:00Z"), Duration.ofDays(1)));

        assertEquals(1_000_000 * 366.0 / 365.0, totals.requests(), 1_000_000 * EXACT);
    }

    /** The scale is the average year's: a given year holds its own count of applicable days. */
    @Test
    void spreadsTheVolumeOverTheApplicableDays()
    {
        ConstantScenario weekdays = new ConstantScenario(1.0, new ApplicabilityFilter(WEEKDAYS, Set.of()));
        LLMStaticAsset asset = asset("llm-weekdays", model(1_000_000, 800, weekdays));
        Totals year = this.totals(asset, new TimeRange(YEAR_2026, YEAR_2027, Duration.ofDays(1)));

        /* 2026 holds 261 weekdays, an average 365-day year 365 × 5 / 7 */
        assertEquals(1_000_000 * 261 / (365.0 * 5 / 7), year.requests(), 1_000_000 * EXACT);
        Totals weekend = this.totals(asset, new TimeRange(Instant.parse("2026-11-07T00:00:00Z"), Instant.parse("2026-11-09T00:00:00Z"), Duration.ofDays(1)));
        assertEquals(0.0, weekend.requests());
    }

    @Test
    void holdsTheWholeVolumeInTheApplicableMonths()
    {
        ConstantScenario january = new ConstantScenario(1.0, new ApplicabilityFilter(Set.of(), Set.of(Month.JANUARY)));
        LLMStaticAsset asset = asset("llm-january", model(1_000_000, 800, january));

        assertTotals(1_000_000, 800, this.totals(asset, new TimeRange(YEAR_2026, Instant.parse("2026-02-01T00:00:00Z"), Duration.ofDays(1))), "over January");
    }

    @Test
    void holdsTheSameTotalsAtEveryResolution()
    {
        LLMStaticAsset asset = asset("llm-resolutions", officeWeek());
        /* 20 days, which 1 h, 1 d and 5 d all split whole */
        TimeRange range = new TimeRange(Instant.parse("2026-11-02T00:00:00Z"), Instant.parse("2026-11-22T00:00:00Z"));

        Totals hourly = this.totals(asset, new TimeRange(range.start(), range.end(), Duration.ofHours(1)));
        for (Duration resolution : RESOLUTIONS)
        {
            Totals totals = this.totals(asset, new TimeRange(range.start(), range.end(), resolution));
            assertEquals(hourly.requests(), totals.requests(), hourly.requests() * EXACT, "requests at " + resolution);
            /* Tokens are rounded per slot: half a token per hourly slot at most */
            assertEquals(hourly.tokens(), totals.tokens(), 0.5 * hourly.slots(), "tokens at " + resolution);
        }
    }

    /** The busiest scenario draws the shape, and the level sets its height: a constant at 0.1 under a bell at 1.0. */
    @Test
    void followsTheScenariosLevels()
    {
        LLMStaticAsset asset = asset("llm-levels", officeWeek());
        List<LLMActivityData> monday = this.slots(asset, new TimeRange(Instant.parse("2026-11-02T00:00:00Z"), Instant.parse("2026-11-03T00:00:00Z"), Duration.ofHours(1)));

        double night = monday.get(3).requestCount();
        /* 13:00 to 14:00, whose midpoint is the bell's peak */
        double peak = monday.get(13).requestCount();
        assertEquals(0.1, night / peak, EXACT);
        /* 09:00 to 10:00, where the bell is still under the constant */
        assertEquals(night, monday.get(9).requestCount(), night * EXACT);
    }

    @Test
    void defaultsTheLevelToOne()
    {
        assertEquals(Scenario.DEFAULT_LEVEL, new ConstantScenario().level());
        assertEquals(1.0, new ConstantScenario(null, null).level());
        assertEquals(1.0, new UserActivityScenario(null, LocalTime.of(9, 0), LocalTime.of(18, 0), null, 0.0, null).level());
        assertEquals(1.0, new BatchActivityScenario(null, LocalTime.of(2, 0), Duration.ofHours(2), 0.0, null, null).level());
    }

    @Test
    void holdsTheAnnualVolumeOfABatch()
    {
        BatchActivityScenario batch = new BatchActivityScenario(1.0, LocalTime.of(2, 0), Duration.ofHours(3), BatchActivityScenario.SmoothingMode.SMOOTH_STEP);
        LLMStaticAsset asset = asset("llm-batch", model(50_000, 1_200, batch));

        assertTotals(50_000, 1_200, this.totals(asset, new TimeRange(YEAR_2026, YEAR_2027, Duration.ofDays(1))), "at P1D");
        List<LLMActivityData> day = this.slots(asset, new TimeRange(Instant.parse("2026-11-02T00:00:00Z"), Instant.parse("2026-11-03T00:00:00Z"), Duration.ofHours(1)));
        assertEquals(0.0, day.get(12).requestCount());
    }

    /** No scenario ever applies: no request, rather than a division by zero. */
    @Test
    void resolvesNoRequestForAFlatShape()
    {
        LLMStaticAsset asset = asset("llm-flat", model(1_000_000, 800, new ConstantScenario(0.0)));

        assertEquals(0.0, this.totals(asset, new TimeRange(YEAR_2026, YEAR_2027, Duration.ofDays(1))).requests());
    }

    private Totals totals(LLMStaticAsset asset, TimeRange range)
    {
        List<LLMActivityData> slots = this.slots(asset, range);
        return new Totals(
            slots.stream().mapToDouble(LLMActivityData::requestCount).sum(),
            slots.stream().mapToDouble(LLMActivityData::outputTokenCount).sum(),
            slots.size()
        );
    }

    private List<LLMActivityData> slots(LLMStaticAsset asset, TimeRange range)
    {
        return this.resolver.resolve(asset, range, null).orElseThrow().activity().stream()
            .map(a -> (LLMActivityData) a)
            .toList();
    }

    /** Requests exactly, tokens within half a token per slot, as each slot rounds its own. */
    private static void assertTotals(long requestPerYear, int outputTokenPerRequest, Totals totals, String context)
    {
        assertEquals(requestPerYear, totals.requests(), requestPerYear * EXACT, "requests " + context);
        assertEquals((double) requestPerYear * outputTokenPerRequest, totals.tokens(), 0.5 * totals.slots() + 1.0, "tokens " + context);
    }

    private static LLMStaticAsset asset(String id, LLMStaticProfile.ModelConfig... models)
    {
        return new LLMStaticAsset(AssetIdentity.of(id, "env-test", LLMStaticModule.TYPE), new LLMStaticProfile(ZoneOffset.UTC, List.of(models)));
    }

    private static LLMStaticProfile.ModelConfig model(long requestPerYear, int outputTokenPerRequest, Scenario... scenarios)
    {
        return model(LLMProvider.openai, "gpt-4o", requestPerYear, outputTokenPerRequest, scenarios);
    }

    private static LLMStaticProfile.ModelConfig model(LLMProvider provider, String name, long requestPerYear, int outputTokenPerRequest, Scenario... scenarios)
    {
        return new LLMStaticProfile.ModelConfig(provider, name, Country.FRANCE, requestPerYear, outputTokenPerRequest, List.of(scenarios));
    }

    /* A constant at a tenth of an office-hours bell on weekdays: the shape the samples ship */
    private static LLMStaticProfile.ModelConfig officeWeek()
    {
        return model(
            1_000_000,
            800,
            new ConstantScenario(0.1),
            new UserActivityScenario(1.0, LocalTime.of(9, 0), LocalTime.of(18, 0), UserActivityScenario.CurveShape.BELL, 0.0, new ApplicabilityFilter(WEEKDAYS, Set.of()))
        );
    }

    private record Totals(double requests, double tokens, int slots) {}
}
