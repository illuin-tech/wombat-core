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
    void runsAtItsLevelOverItsWindow()
    {
        BatchActivityScenario scenario = new BatchActivityScenario(0.5, LocalTime.of(2, 0), Duration.ofHours(2), BatchActivityScenario.SmoothingMode.NONE);

        assertEquals(0.0, scenario.shape(ZonedDateTime.of(2026, 11, 4, 5, 0, 0, 0, ZoneOffset.UTC)));
        assertEquals(0.5, scenario.shape(ZonedDateTime.of(2026, 11, 4, 3, 0, 0, 0, ZoneOffset.UTC)), 1e-12);
    }

    @Test
    void rampsAtBothEnds()
    {
        /* 10 h with a linear ramp over 10% of it: an hour each way */
        BatchActivityScenario scenario = new BatchActivityScenario(1.0, LocalTime.of(2, 0), Duration.ofHours(10), BatchActivityScenario.SmoothingMode.LINEAR_RAMP);

        assertEquals(0.5, scenario.shape(ZonedDateTime.of(2026, 11, 4, 2, 30, 0, 0, ZoneOffset.UTC)), 1e-12);
        assertEquals(1.0, scenario.shape(ZonedDateTime.of(2026, 11, 4, 7, 0, 0, 0, ZoneOffset.UTC)), 1e-12);
    }

    @Test
    void appliesOnlyWhereItsFilterDoes()
    {
        ApplicabilityFilter filter = new ApplicabilityFilter(Set.of(), Set.of(Month.JANUARY));
        BatchActivityScenario scenario = new BatchActivityScenario(1.0, LocalTime.of(2, 0), Duration.ofHours(2), 0.0, BatchActivityScenario.SmoothingMode.NONE, filter);

        assertEquals(1.0, scenario.shape(ZonedDateTime.of(2026, 1, 10, 3, 0, 0, 0, ZoneOffset.UTC)), 1e-12);
        assertEquals(0.0, scenario.shape(ZonedDateTime.of(2026, 2, 10, 3, 0, 0, 0, ZoneOffset.UTC)));
    }

    @Test
    void defaultValuesAreAssignedWhenNullProvided()
    {
        BatchActivityScenario scenario = new BatchActivityScenario(null, LocalTime.of(2, 0), Duration.ofHours(1), 0.0, null, null);

        assertEquals(Scenario.DEFAULT_LEVEL, scenario.level());
        assertEquals(BatchActivityScenario.DEFAULT_SMOOTHING, scenario.smoothing());
        assertEquals(ApplicabilityFilter.ALWAYS, scenario.applicability());
    }

    @Test
    void anEmptyWindowIsFlat()
    {
        BatchActivityScenario zeroDuration = new BatchActivityScenario(1.0, LocalTime.of(2, 0), Duration.ZERO);

        assertEquals(0.0, zeroDuration.shape(ZonedDateTime.of(2026, 11, 4, 2, 0, 0, 0, ZoneOffset.UTC)));
    }
}
