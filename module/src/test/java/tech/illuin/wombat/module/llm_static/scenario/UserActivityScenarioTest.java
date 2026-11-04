package tech.illuin.wombat.module.llm_static.scenario;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.module.llm_static.scenario.tools.ApplicabilityFilter;

import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserActivityScenarioTest
{
    @Test
    void evaluatesRatesFromAnnualTotalAndCurve()
    {
        // 9h window (09:00 - 18:00) -> 9/24 = 0.375
        // BELL curve -> integralFactor = 0.5
        // Active fraction = 0.375 * 0.5 = 0.1875
        // requestPerYear = 100_000 -> peakRate = 100_000 / 0.1875 = 533_333.3333333334
        UserActivityScenario scenario = new UserActivityScenario(
            200,
            100_000,
            LocalTime.of(9, 0),
            LocalTime.of(18, 0),
            UserActivityScenario.CurveShape.BELL,
            0.0
        );

        double expectedPeakRate = 100_000 / 0.1875;

        // Outside window -> 0.0
        ZonedDateTime night = ZonedDateTime.of(2026, 11, 4, 3, 0, 0, 0, ZoneOffset.UTC);
        assertEquals(0.0, scenario.evaluateRequestPerYear(night));

        // Midday (13:30) -> peakRate
        ZonedDateTime midday = ZonedDateTime.of(2026, 11, 4, 13, 30, 0, 0, ZoneOffset.UTC);
        assertEquals(expectedPeakRate, scenario.evaluateRequestPerYear(midday), 1e-4);
    }

    @Test
    void evaluatesRatesWithApplicabilityFilter()
    {
        // January only: 31 / 365
        ApplicabilityFilter filter = new ApplicabilityFilter(Set.of(), Set.of(Month.JANUARY));
        UserActivityScenario scenario = new UserActivityScenario(
            200,
            100_000,
            LocalTime.of(9, 0),
            LocalTime.of(18, 0),
            UserActivityScenario.CurveShape.BELL,
            0.0,
            filter
        );

        double expectedPeakRate = 100_000.0 / ((31.0 / 365.0) * (9.0 / 24.0) * 0.5);

        // In January at midpoint -> peakRate
        ZonedDateTime janMidday = ZonedDateTime.of(2026, 1, 15, 13, 30, 0, 0, ZoneOffset.UTC);
        assertEquals(expectedPeakRate, scenario.evaluateRequestPerYear(janMidday), 1e-4);

        // In February at midpoint -> 0.0 (not applicable)
        ZonedDateTime febMidday = ZonedDateTime.of(2026, 2, 15, 13, 30, 0, 0, ZoneOffset.UTC);
        assertEquals(0.0, scenario.evaluateRequestPerYear(febMidday));
    }

    @Test
    void defaultValuesAreAssignedWhenNullProvided()
    {
        UserActivityScenario scenario = new UserActivityScenario(
            100,
            10_000,
            LocalTime.of(9, 0),
            LocalTime.of(18, 0),
            null,
            0.0,
            null
        );

        assertEquals(UserActivityScenario.DEFAULT_CURVE, scenario.curve());
        assertEquals(ApplicabilityFilter.ALWAYS, scenario.applicability());
    }

    @Test
    void edgeCasesHandleZeroActiveFractionGracefully()
    {
        // Equal opening and closing time -> evaluates to 0
        UserActivityScenario zeroWindow = new UserActivityScenario(
            100,
            50_000,
            LocalTime.of(9, 0),
            LocalTime.of(9, 0),
            UserActivityScenario.CurveShape.BELL,
            0.0
        );
        assertEquals(0.0, zeroWindow.evaluateRequestPerYear(ZonedDateTime.of(2026, 11, 4, 9, 0, 0, 0, ZoneOffset.UTC)));

        // Zero request per year -> evaluates to 0
        UserActivityScenario zeroRequests = new UserActivityScenario(
            100,
            0,
            LocalTime.of(9, 0),
            LocalTime.of(18, 0),
            UserActivityScenario.CurveShape.BELL,
            0.0
        );
        assertEquals(0.0, zeroRequests.evaluateRequestPerYear(ZonedDateTime.of(2026, 11, 4, 13, 30, 0, 0, ZoneOffset.UTC)));
    }
}
