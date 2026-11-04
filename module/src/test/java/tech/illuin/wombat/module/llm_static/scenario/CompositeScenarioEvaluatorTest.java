package tech.illuin.wombat.module.llm_static.scenario;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.module.llm_static.scenario.evaluator.CompositeScenarioEvaluator;
import tech.illuin.wombat.module.llm_static.scenario.evaluator.ScenarioEvaluator;
import tech.illuin.wombat.module.llm_static.scenario.tools.ApplicabilityFilter;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class CompositeScenarioEvaluatorTest
{
    private static final ZoneId UTC = ZoneOffset.UTC;

    private final ScenarioEvaluator evaluator = new CompositeScenarioEvaluator();

    @Test
    void emptyOrNullScenariosReturnsZero()
    {
        ZonedDateTime dt = ZonedDateTime.of(2026, 11, 1, 12, 0, 0, 0, UTC);
        assertEquals(ScenarioResult.EMPTY, this.evaluator.evaluate(dt, null));
        assertEquals(ScenarioResult.EMPTY, this.evaluator.evaluate(dt, Collections.emptyList()));
        assertEquals(ScenarioResult.EMPTY, this.evaluator.evaluate(null, List.of(new ConstantScenario(100, 5000.0, null))));
    }

    @Test
    void constantScenarioEvaluatesLoadCorrectly()
    {
        ConstantScenario scenario = new ConstantScenario(250, 10_000.0, null);
        ZonedDateTime dt = ZonedDateTime.of(2026, 11, 1, 14, 0, 0, 0, UTC);
        assertEquals(10000.0, scenario.evaluateRequestPerYear(dt));
        assertEquals(250, scenario.outputTokenPerRequest());

        ScenarioResult result = this.evaluator.evaluate(dt, List.of(scenario));
        assertEquals(10000.0, result.requestPerYear());
        assertEquals(250, result.outputTokenPerRequest());
    }

    @Test
    void applicabilityFilterEnforcesDayOfWeekAndMonth()
    {
        ApplicabilityFilter filter = new ApplicabilityFilter(
            Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            Set.of(Month.NOVEMBER)
        );
        ConstantScenario scenario = new ConstantScenario(150, 20_000.0, filter);

        // 2026-11-04 is a Wednesday -> Applicable
        ZonedDateTime wednesday = ZonedDateTime.of(2026, 11, 4, 10, 0, 0, 0, UTC);
        assertTrue(scenario.isApplicable(wednesday));
        ScenarioResult resultWed = this.evaluator.evaluate(wednesday, List.of(scenario));
        assertEquals(20000.0, resultWed.requestPerYear());
        assertEquals(150, resultWed.outputTokenPerRequest());

        // 2026-11-05 is a Thursday -> Not applicable
        ZonedDateTime thursday = ZonedDateTime.of(2026, 11, 5, 10, 0, 0, 0, UTC);
        assertFalse(scenario.isApplicable(thursday));
        assertEquals(ScenarioResult.EMPTY, this.evaluator.evaluate(thursday, List.of(scenario)));

        // 2026-12-02 is a Wednesday in December -> Not applicable
        ZonedDateTime decemberWed = ZonedDateTime.of(2026, 12, 2, 10, 0, 0, 0, UTC);
        assertFalse(scenario.isApplicable(decemberWed));
        assertEquals(ScenarioResult.EMPTY, this.evaluator.evaluate(decemberWed, List.of(scenario)));
    }

    @Test
    void userActivityScenarioModulatesBetweenZeroAndPeak()
    {
        UserActivityScenario scenario = new UserActivityScenario(
            200,
            10000.0,
            LocalTime.of(9, 0),
            LocalTime.of(18, 0),
            UserActivityScenario.CurveShape.SINUSOIDAL,
            0.0,
            null
        );

        // Midnight -> 0.0
        ZonedDateTime night = ZonedDateTime.of(2026, 11, 1, 3, 0, 0, 0, UTC);
        assertEquals(0.0, scenario.evaluateRequestPerYear(night));

        // Midday (13:30 is exact midpoint of 9:00 - 18:00) -> Peak
        double peakRate = 10000.0 / ((9.0 / 24.0) * (2.0 / Math.PI));
        ZonedDateTime midday = ZonedDateTime.of(2026, 11, 1, 13, 30, 0, 0, UTC);
        assertEquals(peakRate, scenario.evaluateRequestPerYear(midday), 1e-4);

        // Opening time (9:00) -> 0.0
        ZonedDateTime opening = ZonedDateTime.of(2026, 11, 1, 9, 0, 0, 0, UTC);
        assertEquals(0.0, scenario.evaluateRequestPerYear(opening), 1e-4);

        // Closing time (18:00) -> 0.0
        ZonedDateTime closing = ZonedDateTime.of(2026, 11, 1, 18, 0, 0, 0, UTC);
        assertEquals(0.0, scenario.evaluateRequestPerYear(closing), 1e-4);
    }

    @Test
    void userActivityWithTrapezoidalAndBellCurves()
    {
        UserActivityScenario trapezoid = new UserActivityScenario(
            300,
            10000.0,
            LocalTime.of(10, 0),
            LocalTime.of(20, 0),
            UserActivityScenario.CurveShape.TRAPEZOIDAL,
            0.0,
            null
        );

        double peakRate = 10000.0 / ((10.0 / 24.0) * 0.8);

        // 10:00 -> 0.0 (progress = 0)
        assertEquals(0.0, trapezoid.evaluateRequestPerYear(ZonedDateTime.of(2026, 11, 1, 10, 0, 0, 0, UTC)), 1e-4);
        // 11:00 -> 0.5 * peakRate (progress = 0.1 -> ramp is 0.2, so factor = 0.5)
        assertEquals(0.5 * peakRate, trapezoid.evaluateRequestPerYear(ZonedDateTime.of(2026, 11, 1, 11, 0, 0, 0, UTC)), 1e-4);
        // 12:00 -> peakRate (progress = 0.2 -> plateau)
        assertEquals(peakRate, trapezoid.evaluateRequestPerYear(ZonedDateTime.of(2026, 11, 1, 12, 0, 0, 0, UTC)), 1e-4);
        // 15:00 -> peakRate (progress = 0.5 -> plateau)
        assertEquals(peakRate, trapezoid.evaluateRequestPerYear(ZonedDateTime.of(2026, 11, 1, 15, 0, 0, 0, UTC)), 1e-4);
        // 18:00 -> peakRate (progress = 0.8 -> end of plateau)
        assertEquals(peakRate, trapezoid.evaluateRequestPerYear(ZonedDateTime.of(2026, 11, 1, 18, 0, 0, 0, UTC)), 1e-4);
        // 19:00 -> 0.5 * peakRate (progress = 0.9 -> factor = 0.5)
        assertEquals(0.5 * peakRate, trapezoid.evaluateRequestPerYear(ZonedDateTime.of(2026, 11, 1, 19, 0, 0, 0, UTC)), 1e-4);
        // 20:00 -> 0.0 (progress = 1.0)
        assertEquals(0.0, trapezoid.evaluateRequestPerYear(ZonedDateTime.of(2026, 11, 1, 20, 0, 0, 0, UTC)), 1e-4);

        UserActivityScenario bell = new UserActivityScenario(
            300,
            10_000.0,
            LocalTime.of(10, 0),
            LocalTime.of(20, 0),
            UserActivityScenario.CurveShape.BELL,
            0.0,
            null
        );

        // Midpoint at 15:00 -> Peak
        double bellPeakRate = 10_000.0 / ((10.0 / 24.0) * 0.5);
        assertEquals(bellPeakRate, bell.evaluateRequestPerYear(ZonedDateTime.of(2026, 11, 1, 15, 0, 0, 0, UTC)), 1e-4);
    }

    @Test
    void userActivityCrossesMidnightCorrectly()
    {
        UserActivityScenario overnight = new UserActivityScenario(
            400,
            10000.0,
            LocalTime.of(22, 0),
            LocalTime.of(4, 0),
            UserActivityScenario.CurveShape.SINUSOIDAL,
            0.0,
            null
        );

        // Total window: 22:00 to 04:00 (6 hours)
        // Midpoint: 01:00 (3 hours elapsed)
        double overnightPeakRate = 10000.0 / ((6.0 / 24.0) * (2.0 / Math.PI));
        ZonedDateTime midpoint = ZonedDateTime.of(2026, 11, 1, 1, 0, 0, 0, UTC);
        assertEquals(overnightPeakRate, overnight.evaluateRequestPerYear(midpoint), 1e-4);

        // Outside window: 12:00 -> 0.0
        ZonedDateTime midday = ZonedDateTime.of(2026, 11, 1, 12, 0, 0, 0, UTC);
        assertEquals(0.0, overnight.evaluateRequestPerYear(midday), 1e-4);
    }

    @Test
    void batchActivityEvaluatesWindowCorrectly()
    {
        BatchActivityScenario batch = new BatchActivityScenario(
            500,
            50000.0,
            LocalTime.of(2, 0),
            Duration.ofHours(3),
            0.0,
            BatchActivityScenario.SmoothingMode.NONE,
            null
        );

        // Outside batch window: 01:30 -> 0.0
        ZonedDateTime before = ZonedDateTime.of(2026, 11, 1, 1, 30, 0, 0, UTC);
        assertEquals(0.0, batch.evaluateRequestPerYear(before));

        // Inside batch window: 03:00 -> Peak
        double batchPeakRate = 50000.0 / ((3.0 / 24.0) * 1.0);
        ZonedDateTime inside = ZonedDateTime.of(2026, 11, 1, 3, 0, 0, 0, UTC);
        assertEquals(batchPeakRate, batch.evaluateRequestPerYear(inside));

        // After batch window: 05:30 -> 0.0
        ZonedDateTime after = ZonedDateTime.of(2026, 11, 1, 5, 30, 0, 0, UTC);
        assertEquals(0.0, batch.evaluateRequestPerYear(after));
    }

    @Test
    void batchActivityWithLinearRampAndSmoothStep()
    {
        BatchActivityScenario rampBatch = new BatchActivityScenario(
            600,
            10000.0,
            LocalTime.of(2, 0),
            Duration.ofHours(10), // ramp is 10% = 1 hour
            0.0,
            BatchActivityScenario.SmoothingMode.LINEAR_RAMP,
            null
        );

        double peakRate = 10000.0 / ((10.0 / 24.0) * 0.9);

        // At trigger (02:00, progress 0.0) -> 0.0
        assertEquals(0.0, rampBatch.evaluateRequestPerYear(ZonedDateTime.of(2026, 11, 1, 2, 0, 0, 0, UTC)), 1e-4);
        // Halfway through ramp (02:30, progress 0.05 / 0.1 -> 0.5) -> 0.5 * peakRate
        assertEquals(0.5 * peakRate, rampBatch.evaluateRequestPerYear(ZonedDateTime.of(2026, 11, 1, 2, 30, 0, 0, UTC)), 1e-4);
        // Plateau (04:00, progress 0.2) -> peakRate
        assertEquals(peakRate, rampBatch.evaluateRequestPerYear(ZonedDateTime.of(2026, 11, 1, 4, 0, 0, 0, UTC)), 1e-4);

        BatchActivityScenario smoothBatch = new BatchActivityScenario(
            600,
            10000.0,
            LocalTime.of(2, 0),
            Duration.ofHours(10),
            0.0,
            BatchActivityScenario.SmoothingMode.SMOOTH_STEP,
            null
        );

        // At midpoint plateau (07:00, progress 0.5) -> peakRate
        double smoothPeakRate = 10000.0 / ((10.0 / 24.0) * 0.9);
        assertEquals(smoothPeakRate, smoothBatch.evaluateRequestPerYear(ZonedDateTime.of(2026, 11, 1, 7, 0, 0, 0, UTC)), 1e-4);
    }

    @Test
    void batchActivityCrossesMidnightCorrectly()
    {
        BatchActivityScenario midnightBatch = new BatchActivityScenario(
            700,
            8000.0,
            LocalTime.of(23, 0),
            Duration.ofHours(4), // runs 23:00 to 03:00 next day
            0.0,
            BatchActivityScenario.SmoothingMode.NONE,
            null
        );

        double midnightPeakRate = 8000.0 / ((4.0 / 24.0) * 1.0);

        // 23:30 (Day 1) -> Inside batch
        ZonedDateTime day1 = ZonedDateTime.of(2026, 11, 1, 23, 30, 0, 0, UTC);
        assertEquals(midnightPeakRate, midnightBatch.evaluateRequestPerYear(day1));

        // 01:30 (Day 2) -> Inside batch
        ZonedDateTime day2 = ZonedDateTime.of(2026, 11, 2, 1, 30, 0, 0, UTC);
        assertEquals(midnightPeakRate, midnightBatch.evaluateRequestPerYear(day2));

        // 04:00 (Day 2) -> Outside batch -> 0.0
        ZonedDateTime day2After = ZonedDateTime.of(2026, 11, 2, 4, 0, 0, 0, UTC);
        assertEquals(0.0, midnightBatch.evaluateRequestPerYear(day2After));
    }

    @Test
    void compositeScenarioEvaluatesEnvelopeOfConstantUserAndBatch()
    {
        ConstantScenario baseline = new ConstantScenario(100, 1000.0, null);
        UserActivityScenario userActivity = new UserActivityScenario(
            200,
            10000.0,
            LocalTime.of(8, 0),
            LocalTime.of(18, 0),
            UserActivityScenario.CurveShape.SINUSOIDAL,
            0.0,
            null
        );
        BatchActivityScenario batch = new BatchActivityScenario(
            500,
            25000.0,
            LocalTime.of(2, 0),
            Duration.ofHours(2),
            0.0,
            BatchActivityScenario.SmoothingMode.NONE,
            null
        );

        List<Scenario> scenarios = List.of(baseline, userActivity, batch);

        // Night off-hours (05:00): baseline (1000.0) > userActivity (0.0) > batch (0.0)
        ZonedDateTime offHours = ZonedDateTime.of(2026, 11, 1, 5, 0, 0, 0, UTC);
        ScenarioResult resOffHours = this.evaluator.evaluate(offHours, scenarios);
        assertEquals(1000.0, resOffHours.requestPerYear());
        assertEquals(100, resOffHours.outputTokenPerRequest());

        // Batch execution time (02:30): batch is maximal
        ZonedDateTime batchTime = ZonedDateTime.of(2026, 11, 1, 2, 30, 0, 0, UTC);
        ScenarioResult resBatch = this.evaluator.evaluate(batchTime, scenarios);
        assertEquals(batch.evaluateRequestPerYear(batchTime), resBatch.requestPerYear());
        assertEquals(500, resBatch.outputTokenPerRequest());

        // Daytime peak (13:00): userActivity is maximal
        ZonedDateTime dayPeak = ZonedDateTime.of(2026, 11, 1, 13, 0, 0, 0, UTC);
        ScenarioResult resDay = this.evaluator.evaluate(dayPeak, scenarios);
        assertEquals(userActivity.evaluateRequestPerYear(dayPeak), resDay.requestPerYear());
        assertEquals(200, resDay.outputTokenPerRequest());
    }
}
