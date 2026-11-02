package tech.illuin.wombat.module.kubernetes_static.scenario;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.module.kubernetes_static.scenario.evaluator.CompositeScenarioEvaluator;
import tech.illuin.wombat.module.kubernetes_static.scenario.evaluator.ScenarioEvaluator;
import tech.illuin.wombat.module.kubernetes_static.scenario.tools.ApplicabilityFilter;

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
        assertEquals(0.0, this.evaluator.evaluate(dt, null));
        assertEquals(0.0, this.evaluator.evaluate(dt, Collections.emptyList()));
        assertEquals(0.0, this.evaluator.evaluate(null, List.of(new ConstantScenario(0.5, null))));
    }

    @Test
    void constantScenarioEvaluatesLoadCorrectly()
    {
        ConstantScenario scenario = new ConstantScenario(0.25, null);
        ZonedDateTime dt = ZonedDateTime.of(2026, 11, 1, 14, 0, 0, 0, UTC);
        assertEquals(0.25, scenario.evaluateLoad(dt));
    }

    @Test
    void applicabilityFilterEnforcesDayOfWeekAndMonth()
    {
        ApplicabilityFilter filter = new ApplicabilityFilter(
            Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            Set.of(Month.NOVEMBER)
        );
        ConstantScenario scenario = new ConstantScenario(0.40, filter);

        // 2026-11-04 is a Wednesday -> Applicable
        ZonedDateTime wednesday = ZonedDateTime.of(2026, 11, 4, 10, 0, 0, 0, UTC);
        assertTrue(scenario.isApplicable(wednesday));
        assertEquals(0.40, this.evaluator.evaluate(wednesday, List.of(scenario)));

        // 2026-11-05 is a Thursday -> Not applicable
        ZonedDateTime thursday = ZonedDateTime.of(2026, 11, 5, 10, 0, 0, 0, UTC);
        assertFalse(scenario.isApplicable(thursday));
        assertEquals(0.0, this.evaluator.evaluate(thursday, List.of(scenario)));

        // 2026-12-02 is a Wednesday in December -> Not applicable
        ZonedDateTime decemberWed = ZonedDateTime.of(2026, 12, 2, 10, 0, 0, 0, UTC);
        assertFalse(scenario.isApplicable(decemberWed));
        assertEquals(0.0, this.evaluator.evaluate(decemberWed, List.of(scenario)));
    }

    @Test
    void userActivityScenarioModulatesBetweenBaselineAndPeak()
    {
        UserActivityScenario scenario = new UserActivityScenario(
            0.10,
            0.75,
            LocalTime.of(9, 0),
            LocalTime.of(18, 0),
            UserActivityScenario.CurveShape.SINUSOIDAL,
            0.0,
            null
        );

        // Midnight -> Baseline (0.10)
        ZonedDateTime night = ZonedDateTime.of(2026, 11, 1, 3, 0, 0, 0, UTC);
        assertEquals(0.10, scenario.evaluateLoad(night));

        // Midday (13:30 is exact midpoint of 9:00 - 18:00) -> Peak (0.75)
        ZonedDateTime midday = ZonedDateTime.of(2026, 11, 1, 13, 30, 0, 0, UTC);
        assertEquals(0.75, scenario.evaluateLoad(midday), 1e-4);

        // Opening time (9:00) -> Baseline (0.10)
        ZonedDateTime opening = ZonedDateTime.of(2026, 11, 1, 9, 0, 0, 0, UTC);
        assertEquals(0.10, scenario.evaluateLoad(opening), 1e-4);

        // Closing time (18:00) -> Baseline (0.10)
        ZonedDateTime closing = ZonedDateTime.of(2026, 11, 1, 18, 0, 0, 0, UTC);
        assertEquals(0.10, scenario.evaluateLoad(closing), 1e-4);
    }

    @Test
    void userActivityWithTrapezoidalAndBellCurves()
    {
        UserActivityScenario trapezoid = new UserActivityScenario(
            0.0,
            1.0,
            LocalTime.of(10, 0),
            LocalTime.of(20, 0),
            UserActivityScenario.CurveShape.TRAPEZOIDAL,
            0.0,
            null
        );

        // Total window = 10h (600m). Ramp = 20% (2h / 120m).
        // At 11:00 (1h / 10% progress) -> factor = 0.5 -> 0.5
        ZonedDateTime rampUp = ZonedDateTime.of(2026, 11, 1, 11, 0, 0, 0, UTC);
        assertEquals(0.5, trapezoid.evaluateLoad(rampUp), 1e-4);

        // At 15:00 (5h / 50% progress) -> factor = 1.0 -> 1.0
        ZonedDateTime plateau = ZonedDateTime.of(2026, 11, 1, 15, 0, 0, 0, UTC);
        assertEquals(1.0, trapezoid.evaluateLoad(plateau), 1e-4);

        // Bell curve test
        UserActivityScenario bell = new UserActivityScenario(
            0.0,
            1.0,
            LocalTime.of(10, 0),
            LocalTime.of(20, 0),
            UserActivityScenario.CurveShape.BELL,
            0.0,
            null
        );
        assertEquals(1.0, bell.evaluateLoad(plateau), 1e-4);
    }

    @Test
    void batchActivityScenarioHandlesTriggerAndDuration()
    {
        BatchActivityScenario batch = new BatchActivityScenario(
            0.10,
            0.90,
            LocalTime.of(2, 0),
            Duration.ofHours(2),
            0.0,
            BatchActivityScenario.SmoothingMode.NONE,
            null
        );

        // Before trigger (01:30) -> baseline 0.10
        ZonedDateTime before = ZonedDateTime.of(2026, 11, 1, 1, 30, 0, 0, UTC);
        assertEquals(0.10, batch.evaluateLoad(before));

        // During batch (03:00) -> peak 0.90
        ZonedDateTime during = ZonedDateTime.of(2026, 11, 1, 3, 0, 0, 0, UTC);
        assertEquals(0.90, batch.evaluateLoad(during));

        // After batch (04:30) -> baseline 0.10
        ZonedDateTime after = ZonedDateTime.of(2026, 11, 1, 4, 30, 0, 0, UTC);
        assertEquals(0.10, batch.evaluateLoad(after));
    }

    @Test
    void batchActivityScenarioHandlesOvernightCrossingMidnight()
    {
        BatchActivityScenario overnight = new BatchActivityScenario(
            0.0,
            0.80,
            LocalTime.of(23, 0),
            Duration.ofHours(3),
            0.0,
            BatchActivityScenario.SmoothingMode.NONE,
            null
        );

        // 23:30 (Day 1) -> active (0.80)
        ZonedDateTime d1 = ZonedDateTime.of(2026, 11, 1, 23, 30, 0, 0, UTC);
        assertEquals(0.80, overnight.evaluateLoad(d1));

        // 01:00 (Day 2) -> active (0.80)
        ZonedDateTime d2 = ZonedDateTime.of(2026, 11, 2, 1, 0, 0, 0, UTC);
        assertEquals(0.80, overnight.evaluateLoad(d2));

        // 02:30 (Day 2) -> inactive (0.0)
        ZonedDateTime d3 = ZonedDateTime.of(2026, 11, 2, 2, 30, 0, 0, UTC);
        assertEquals(0.0, overnight.evaluateLoad(d3));
    }

    @Test
    void batchActivityWithSmoothingProfiles()
    {
        BatchActivityScenario linearBatch = new BatchActivityScenario(
            0.0,
            1.0,
            LocalTime.of(2, 0),
            Duration.ofMinutes(100),
            0.0,
            BatchActivityScenario.SmoothingMode.LINEAR_RAMP,
            null
        );

        // At 2:05 (progress 0.05, ramp is 0.10) -> factor = 0.5 -> 0.5
        ZonedDateTime rampUp = ZonedDateTime.of(2026, 11, 1, 2, 5, 0, 0, UTC);
        assertEquals(0.5, linearBatch.evaluateLoad(rampUp), 1e-4);

        // At 2:50 (progress 0.50) -> factor = 1.0 -> 1.0
        ZonedDateTime mid = ZonedDateTime.of(2026, 11, 1, 2, 50, 0, 0, UTC);
        assertEquals(1.0, linearBatch.evaluateLoad(mid), 1e-4);

        // Smooth step
        BatchActivityScenario smoothBatch = new BatchActivityScenario(
            0.0,
            1.0,
            LocalTime.of(2, 0),
            Duration.ofMinutes(100),
            0.0,
            BatchActivityScenario.SmoothingMode.SMOOTH_STEP,
            null
        );
        // At 2:05: x = 0.5 -> smoothstep(0.5) = 0.25 * (3 - 1) = 0.5 -> 0.5
        assertEquals(0.5, smoothBatch.evaluateLoad(rampUp), 1e-4);
    }

    @Test
    void compositeEvaluatorSuperposesScenariosAndSelectsMaxEnvelope()
    {
        // Scenario 1: baseline 0.10
        ConstantScenario baseline = new ConstantScenario(0.10, null);

        // Scenario 2: Diurnal user activity (peak 0.75 at 14:00)
        UserActivityScenario userActivity = new UserActivityScenario(
            0.10,
            0.75,
            LocalTime.of(9, 0),
            LocalTime.of(19, 0),
            UserActivityScenario.CurveShape.SINUSOIDAL,
            0.0,
            null
        );

        // Scenario 3: Batch nocturnal processing (peak 0.90 at 03:00)
        BatchActivityScenario batchActivity = new BatchActivityScenario(
            0.0,
            0.90,
            LocalTime.of(2, 0),
            Duration.ofHours(2),
            0.0,
            BatchActivityScenario.SmoothingMode.NONE,
            null
        );

        List<Scenario> scenarios = List.of(baseline, userActivity, batchActivity);

        // At 03:00: batch peak (0.90) dominates user activity baseline (0.10) and constant (0.10)
        ZonedDateTime t0300 = ZonedDateTime.of(2026, 11, 1, 3, 0, 0, 0, UTC);
        assertEquals(0.90, this.evaluator.evaluate(t0300, scenarios), 1e-4);

        // At 14:00: user activity peak (0.75) dominates constant baseline (0.10) and batch idle (0.0)
        ZonedDateTime t1400 = ZonedDateTime.of(2026, 11, 1, 14, 0, 0, 0, UTC);
        assertEquals(0.75, this.evaluator.evaluate(t1400, scenarios), 1e-4);

        // At 22:00: constant baseline (0.10) dominates user activity baseline (0.10) and batch idle (0.0)
        ZonedDateTime t2200 = ZonedDateTime.of(2026, 11, 1, 22, 0, 0, 0, UTC);
        assertEquals(0.10, this.evaluator.evaluate(t2200, scenarios), 1e-4);
    }

    @Test
    void defaultValuesAreAssignedInConstructors()
    {
        ConstantScenario constant = new ConstantScenario(1.5, null);
        assertEquals(ApplicabilityFilter.ALWAYS, constant.applicability());

        UserActivityScenario userActivity = new UserActivityScenario(
            0.0,
            1.0,
            LocalTime.of(9, 0),
            LocalTime.of(17, 0),
            null,
            0.0,
            null
        );
        assertEquals(UserActivityScenario.DEFAULT_CURVE, userActivity.curve());
        assertEquals(ApplicabilityFilter.ALWAYS, userActivity.applicability());

        BatchActivityScenario batchActivity = new BatchActivityScenario(
            0.0,
            1.0,
            LocalTime.of(2, 0),
            Duration.ofHours(1),
            0.0,
            null,
            null
        );
        assertEquals(BatchActivityScenario.DEFAULT_SMOOTHING, batchActivity.smoothing());
        assertEquals(ApplicabilityFilter.ALWAYS, batchActivity.applicability());

        ApplicabilityFilter filter = new ApplicabilityFilter(null, null);
        assertEquals(Set.of(), filter.daysOfWeek());
        assertEquals(Set.of(), filter.months());
        assertTrue(filter.isApplicable(ZonedDateTime.now(UTC)));
    }
}
