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
    void peaksAtItsLevel()
    {
        UserActivityScenario scenario = new UserActivityScenario(0.6, LocalTime.of(9, 0), LocalTime.of(18, 0), UserActivityScenario.CurveShape.BELL);

        // Outside window -> 0.0
        assertEquals(0.0, scenario.shape(ZonedDateTime.of(2026, 11, 4, 3, 0, 0, 0, ZoneOffset.UTC)));
        // Midday (13:30, the window's midpoint) -> the level
        assertEquals(0.6, scenario.shape(ZonedDateTime.of(2026, 11, 4, 13, 30, 0, 0, ZoneOffset.UTC)), 1e-12);
        // A quarter into the window: the bell is halfway up
        assertEquals(0.3, scenario.shape(ZonedDateTime.of(2026, 11, 4, 11, 15, 0, 0, ZoneOffset.UTC)), 1e-12);
    }

    @Test
    void appliesOnlyWhereItsFilterDoes()
    {
        ApplicabilityFilter filter = new ApplicabilityFilter(Set.of(), Set.of(Month.JANUARY));
        UserActivityScenario scenario = new UserActivityScenario(1.0, LocalTime.of(9, 0), LocalTime.of(18, 0), UserActivityScenario.CurveShape.BELL, 0.0, filter);

        assertEquals(1.0, scenario.shape(ZonedDateTime.of(2026, 1, 15, 13, 30, 0, 0, ZoneOffset.UTC)), 1e-12);
        assertEquals(0.0, scenario.shape(ZonedDateTime.of(2026, 2, 15, 13, 30, 0, 0, ZoneOffset.UTC)));
    }

    @Test
    void defaultValuesAreAssignedWhenNullProvided()
    {
        UserActivityScenario scenario = new UserActivityScenario(null, LocalTime.of(9, 0), LocalTime.of(18, 0), null, 0.0, null);

        assertEquals(Scenario.DEFAULT_LEVEL, scenario.level());
        assertEquals(UserActivityScenario.DEFAULT_CURVE, scenario.curve());
        assertEquals(ApplicabilityFilter.ALWAYS, scenario.applicability());
    }

    @Test
    void anEmptyWindowIsFlat()
    {
        UserActivityScenario zeroWindow = new UserActivityScenario(1.0, LocalTime.of(9, 0), LocalTime.of(9, 0), UserActivityScenario.CurveShape.BELL);

        assertEquals(0.0, zeroWindow.shape(ZonedDateTime.of(2026, 11, 4, 9, 0, 0, 0, ZoneOffset.UTC)));
    }
}
