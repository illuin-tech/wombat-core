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
    void emptyOrNullScenariosAreFlat()
    {
        ZonedDateTime dt = ZonedDateTime.of(2026, 11, 1, 12, 0, 0, 0, UTC);
        assertEquals(0.0, this.evaluator.evaluate(dt, null));
        assertEquals(0.0, this.evaluator.evaluate(dt, Collections.emptyList()));
        assertEquals(0.0, this.evaluator.evaluate(null, List.of(new ConstantScenario())));
    }

    @Test
    void constantScenarioStaysAtItsLevel()
    {
        ZonedDateTime dt = ZonedDateTime.of(2026, 11, 1, 14, 0, 0, 0, UTC);
        assertEquals(0.25, this.evaluator.evaluate(dt, List.of(new ConstantScenario(0.25))));
    }

    @Test
    void applicabilityFilterEnforcesDayOfWeekAndMonth()
    {
        ApplicabilityFilter filter = new ApplicabilityFilter(
            Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            Set.of(Month.NOVEMBER)
        );
        List<Scenario> scenarios = List.of(new ConstantScenario(1.0, filter));

        // 2026-11-04 is a Wednesday -> applicable
        assertEquals(1.0, this.evaluator.evaluate(ZonedDateTime.of(2026, 11, 4, 10, 0, 0, 0, UTC), scenarios));
        // 2026-11-05 is a Thursday -> not applicable
        assertEquals(0.0, this.evaluator.evaluate(ZonedDateTime.of(2026, 11, 5, 10, 0, 0, 0, UTC), scenarios));
        // 2026-12-02 is a Wednesday in December -> not applicable
        assertEquals(0.0, this.evaluator.evaluate(ZonedDateTime.of(2026, 12, 2, 10, 0, 0, 0, UTC), scenarios));
    }

    @Test
    void userActivityFollowsItsCurve()
    {
        UserActivityScenario sinusoidal = new UserActivityScenario(1.0, LocalTime.of(9, 0), LocalTime.of(18, 0), UserActivityScenario.CurveShape.SINUSOIDAL);
        assertEquals(0.0, sinusoidal.shape(ZonedDateTime.of(2026, 11, 1, 3, 0, 0, 0, UTC)));
        assertEquals(1.0, sinusoidal.shape(ZonedDateTime.of(2026, 11, 1, 13, 30, 0, 0, UTC)), 1e-12);
        assertEquals(0.0, sinusoidal.shape(ZonedDateTime.of(2026, 11, 1, 9, 0, 0, 0, UTC)), 1e-12);
        assertEquals(0.0, sinusoidal.shape(ZonedDateTime.of(2026, 11, 1, 18, 0, 0, 0, UTC)), 1e-12);

        UserActivityScenario trapezoid = new UserActivityScenario(1.0, LocalTime.of(10, 0), LocalTime.of(20, 0), UserActivityScenario.CurveShape.TRAPEZOIDAL);
        // 11:00 -> progress 0.1, half of the 0.2 ramp
        assertEquals(0.5, trapezoid.shape(ZonedDateTime.of(2026, 11, 1, 11, 0, 0, 0, UTC)), 1e-12);
        // 12:00 to 18:00 -> plateau
        assertEquals(1.0, trapezoid.shape(ZonedDateTime.of(2026, 11, 1, 15, 0, 0, 0, UTC)), 1e-12);
        assertEquals(0.5, trapezoid.shape(ZonedDateTime.of(2026, 11, 1, 19, 0, 0, 0, UTC)), 1e-12);
    }

    @Test
    void userActivityCrossesMidnightCorrectly()
    {
        // 22:00 to 04:00, midpoint at 01:00
        UserActivityScenario overnight = new UserActivityScenario(1.0, LocalTime.of(22, 0), LocalTime.of(4, 0), UserActivityScenario.CurveShape.SINUSOIDAL);

        assertEquals(1.0, overnight.shape(ZonedDateTime.of(2026, 11, 1, 1, 0, 0, 0, UTC)), 1e-12);
        assertEquals(0.0, overnight.shape(ZonedDateTime.of(2026, 11, 1, 12, 0, 0, 0, UTC)), 1e-12);
    }

    @Test
    void batchActivityCrossesMidnightCorrectly()
    {
        // 23:00 to 03:00 the next day
        BatchActivityScenario midnightBatch = new BatchActivityScenario(1.0, LocalTime.of(23, 0), Duration.ofHours(4));

        assertEquals(1.0, midnightBatch.shape(ZonedDateTime.of(2026, 11, 1, 23, 30, 0, 0, UTC)));
        assertEquals(1.0, midnightBatch.shape(ZonedDateTime.of(2026, 11, 2, 1, 30, 0, 0, UTC)));
        assertEquals(0.0, midnightBatch.shape(ZonedDateTime.of(2026, 11, 2, 4, 0, 0, 0, UTC)));
    }

    /** The scenarios model one model's behaviour: the busiest draws the shape, levels do not add up. */
    @Test
    void theBusiestScenarioDrawsTheShape()
    {
        ConstantScenario baseline = new ConstantScenario(0.1);
        UserActivityScenario userActivity = new UserActivityScenario(1.0, LocalTime.of(8, 0), LocalTime.of(18, 0), UserActivityScenario.CurveShape.SINUSOIDAL);
        BatchActivityScenario batch = new BatchActivityScenario(0.5, LocalTime.of(2, 0), Duration.ofHours(2));
        List<Scenario> scenarios = List.of(baseline, userActivity, batch);

        // Night off-hours (05:00): the baseline alone
        assertEquals(0.1, this.evaluator.evaluate(ZonedDateTime.of(2026, 11, 1, 5, 0, 0, 0, UTC), scenarios));
        // Batch (02:30): the batch, not the batch plus the baseline
        assertEquals(0.5, this.evaluator.evaluate(ZonedDateTime.of(2026, 11, 1, 2, 30, 0, 0, UTC), scenarios));
        // Daytime peak (13:00): the user activity
        assertEquals(1.0, this.evaluator.evaluate(ZonedDateTime.of(2026, 11, 1, 13, 0, 0, 0, UTC), scenarios), 1e-12);
    }
}
