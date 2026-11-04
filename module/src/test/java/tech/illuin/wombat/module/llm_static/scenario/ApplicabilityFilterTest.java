package tech.illuin.wombat.module.llm_static.scenario;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.module.llm_static.scenario.tools.ApplicabilityFilter;

import java.time.DayOfWeek;
import java.time.Month;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicabilityFilterTest
{
    @Test
    void annualActiveFractionForAlwaysIsOne()
    {
        assertEquals(1.0, ApplicabilityFilter.ALWAYS.annualActiveFraction());
        assertEquals(1.0, new ApplicabilityFilter(null, null).annualActiveFraction());
        assertEquals(1.0, new ApplicabilityFilter(Set.of(), Set.of()).annualActiveFraction());
    }

    @Test
    void annualActiveFractionForJanuaryOnly()
    {
        ApplicabilityFilter filter = new ApplicabilityFilter(Set.of(), Set.of(Month.JANUARY));
        // January has 31 days in a 365-day year
        assertEquals(31.0 / 365.0, filter.annualActiveFraction(), 1e-9);
    }

    @Test
    void annualActiveFractionForWeekdays()
    {
        ApplicabilityFilter filter = new ApplicabilityFilter(
            Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
            Set.of()
        );
        // 2026 starts on Thursday (52 weeks * 5 weekdays + 1 Thursday = 261 days)
        assertEquals(261.0 / 365.0, filter.annualActiveFraction(), 1e-9);
    }

    @Test
    void annualActiveFractionForJanuaryWeekdays()
    {
        ApplicabilityFilter filter = new ApplicabilityFilter(
            Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
            Set.of(Month.JANUARY)
        );
        // In Jan 2026: 31 days, starts on Thursday (5 Thu, 5 Fri, 4 Sat, 4 Sun, 4 Mon, 4 Tue, 4 Wed) -> 22 weekdays
        assertEquals(22.0 / 365.0, filter.annualActiveFraction(), 1e-9);
    }

    @Test
    void curveShapeIntegralFactors()
    {
        assertEquals(2.0 / Math.PI, UserActivityScenario.CurveShape.SINUSOIDAL.integralFactor(), 1e-9);
        assertEquals(0.8, UserActivityScenario.CurveShape.TRAPEZOIDAL.integralFactor(), 1e-9);
        assertEquals(0.5, UserActivityScenario.CurveShape.BELL.integralFactor(), 1e-9);
    }

    @Test
    void smoothingModeIntegralFactors()
    {
        assertEquals(1.0, BatchActivityScenario.SmoothingMode.NONE.integralFactor(), 1e-9);
        assertEquals(0.9, BatchActivityScenario.SmoothingMode.LINEAR_RAMP.integralFactor(), 1e-9);
        assertEquals(0.9, BatchActivityScenario.SmoothingMode.SMOOTH_STEP.integralFactor(), 1e-9);
    }
}
