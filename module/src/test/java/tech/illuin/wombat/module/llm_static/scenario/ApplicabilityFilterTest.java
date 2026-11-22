package tech.illuin.wombat.module.llm_static.scenario;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.module.llm_static.scenario.tools.ApplicabilityFilter;

import java.time.DayOfWeek;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicabilityFilterTest
{
    /* 2026-01-15 is a Thursday */
    private static final ZonedDateTime JANUARY_THURSDAY = ZonedDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void appliesAlwaysWhenEmpty()
    {
        assertEquals(ApplicabilityFilter.ALWAYS, new ApplicabilityFilter(null, null));
        assertTrue(ApplicabilityFilter.ALWAYS.isApplicable(JANUARY_THURSDAY));
    }

    @Test
    void appliesOnItsDaysAndMonths()
    {
        ApplicabilityFilter weekdaysOfJanuary = new ApplicabilityFilter(
            Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
            Set.of(Month.JANUARY)
        );

        assertTrue(weekdaysOfJanuary.isApplicable(JANUARY_THURSDAY));
        assertFalse(weekdaysOfJanuary.isApplicable(JANUARY_THURSDAY.plusDays(2)));
        assertFalse(weekdaysOfJanuary.isApplicable(JANUARY_THURSDAY.plusMonths(1)));
        assertFalse(weekdaysOfJanuary.isApplicable(null));
    }
}
