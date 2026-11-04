package tech.illuin.wombat.module.llm_static.scenario;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.module.llm_static.scenario.tools.ApplicabilityFilter;

import java.time.Duration;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BatchActivityScenarioTest
{
    @Test
    void evaluatesRatesFromAnnualTotalDurationAndSmoothing()
    {
        // 2h duration -> 2/24 = 1/12 day
        // NONE smoothing -> integralFactor = 1.0
        // Active fraction = (2/24) * 1.0 = 1/12
        // requestPerYear = 60_000 -> peakRate = 60_000 * 12 = 720_000
        BatchActivityScenario scenario = new BatchActivityScenario(
            500,
            60_000,
            LocalTime.of(2, 0),
            Duration.ofHours(2),
            0.0,
            BatchActivityScenario.SmoothingMode.NONE
        );

        // Outside batch window -> 0.0
        ZonedDateTime outside = ZonedDateTime.of(2026, 11, 4, 5, 0, 0, 0, ZoneOffset.UTC);
        assertEquals(0.0, scenario.evaluateRequestPerYear(outside));

        // Inside batch window (03:00) -> peakRate (720_000)
        ZonedDateTime inside = ZonedDateTime.of(2026, 11, 4, 3, 0, 0, 0, ZoneOffset.UTC);
        assertEquals(720_000.0, scenario.evaluateRequestPerYear(inside), 1e-4);
    }

    @Test
    void evaluatesRatesWithSmoothingMode()
    {
        // 10h duration, LINEAR_RAMP -> integralFactor = 0.9
        // Active fraction = (10/24) * 0.9 = 9/24 = 0.375
        // requestPerYear = 75_000 -> peakRate = 75_000 / 0.375 = 200_000
        BatchActivityScenario scenario = new BatchActivityScenario(
            300,
            75_000,
            LocalTime.of(2, 0),
            Duration.ofHours(10),
            0.0,
            BatchActivityScenario.SmoothingMode.LINEAR_RAMP
        );

        // At midpoint of plateau (07:00, progress 0.5) -> peakRate
        ZonedDateTime plateau = ZonedDateTime.of(2026, 11, 4, 7, 0, 0, 0, ZoneOffset.UTC);
        assertEquals(200_000.0, scenario.evaluateRequestPerYear(plateau), 1e-4);
    }

    @Test
    void evaluatesRatesWithApplicabilityFilter()
    {
        // January only: 31 / 365
        ApplicabilityFilter filter = new ApplicabilityFilter(Set.of(), Set.of(Month.JANUARY));
        BatchActivityScenario scenario = new BatchActivityScenario(
            400,
            31_000,
            LocalTime.of(2, 0),
            Duration.ofHours(2),
            0.0,
            BatchActivityScenario.SmoothingMode.NONE,
            filter
        );

        double expectedPeakRate = 31_000.0 / ((31.0 / 365.0) * (2.0 / 24.0) * 1.0);

        // In January inside window -> peakRate
        ZonedDateTime janTime = ZonedDateTime.of(2026, 1, 10, 3, 0, 0, 0, ZoneOffset.UTC);
        assertEquals(expectedPeakRate, scenario.evaluateRequestPerYear(janTime), 1e-4);

        // In February inside window -> 0.0
        ZonedDateTime febTime = ZonedDateTime.of(2026, 2, 10, 3, 0, 0, 0, ZoneOffset.UTC);
        assertEquals(0.0, scenario.evaluateRequestPerYear(febTime));
    }

    @Test
    void defaultValuesAreAssignedWhenNullProvided()
    {
        BatchActivityScenario scenario = new BatchActivityScenario(
            100,
            5000,
            LocalTime.of(2, 0),
            Duration.ofHours(1),
            0.0,
            null,
            null
        );

        assertEquals(BatchActivityScenario.DEFAULT_SMOOTHING, scenario.smoothing());
        assertEquals(ApplicabilityFilter.ALWAYS, scenario.applicability());
    }

    @Test
    void edgeCasesHandleZeroDurationGracefully()
    {
        BatchActivityScenario zeroDuration = new BatchActivityScenario(
            100,
            50_000,
            LocalTime.of(2, 0),
            Duration.ZERO
        );
        assertEquals(0.0, zeroDuration.evaluateRequestPerYear(ZonedDateTime.of(2026, 11, 4, 2, 0, 0, 0, ZoneOffset.UTC)));
    }
}
