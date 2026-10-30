package tech.illuin.wombat.core.activity.commons;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TimeRangeTest
{
    private final Instant START = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();
    private final Instant END = ZonedDateTime.of(2026, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC).toInstant(); // 1 day later (24 hours)
    private final Duration DURATION_ONE_DAY = Duration.ofDays(1);
    private final Duration DURATION_ONE_HOUR = Duration.ofHours(1);

    @Test
    void constructor_withValidResolution_createsInstance()
    {
        TimeRange range = new TimeRange(START, END, DURATION_ONE_HOUR);

        assertEquals(START, range.start());
        assertEquals(END, range.end());
        assertEquals(DURATION_ONE_HOUR, range.resolution());
    }

    @Test
    void constructor_withResolutionGreaterThanDuration_throwsException()
    {
        Duration twoDays = Duration.ofDays(2);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new TimeRange(START, END, twoDays);
        });

        assertEquals("Resolution must be less than or equal to the interval defined by the start and end timestamps.", exception.getMessage());
    }

    @Test
    void constructor_withoutResolution_defaultsToFullDuration()
    {
        TimeRange range = new TimeRange(START, END);

        assertEquals(START, range.start());
        assertEquals(END, range.end());
        assertEquals(DURATION_ONE_DAY, range.resolution());
    }

    @Test
    void between_createsCorrectTimeRange()
    {
        TimeRange range = TimeRange.between(START, END);

        assertEquals(START, range.start());
        assertEquals(END, range.end());
        assertEquals(DURATION_ONE_DAY, range.resolution());
    }

    @Test
    void clone_createsExactCopy()
    {
        TimeRange original = new TimeRange(START, END, DURATION_ONE_HOUR);
        TimeRange cloned = TimeRange.clone(original);

        assertEquals(original.start(), cloned.start());
        assertEquals(original.end(), cloned.end());
        assertEquals(original.resolution(), cloned.resolution());
    }

    @Test
    void cloneRescale_withNewResolution_updatesResolutionOnly()
    {
        TimeRange original = new TimeRange(START, END, DURATION_ONE_HOUR);
        TimeRange rescaled = TimeRange.cloneRescale(original, Duration.ofMinutes(30));

        assertEquals(START, rescaled.start());
        assertEquals(END, rescaled.end());
        assertEquals(Duration.ofMinutes(30), rescaled.resolution());
    }

    @Test
    void cloneRescale_withoutNewResolution_resetsToFullDuration()
    {
        TimeRange original = new TimeRange(START, END, DURATION_ONE_HOUR);
        TimeRange rescaled = TimeRange.cloneRescale(original);

        assertEquals(START, rescaled.start());
        assertEquals(END, rescaled.end());
        assertEquals(DURATION_ONE_DAY, rescaled.resolution());
    }

    @Test
    void toEpochMs_handlesMinMaxAndNormalInstants()
    {
        assertEquals(Long.MIN_VALUE, TimeRange.toEpochMs(Instant.MIN));
        assertEquals(Long.MAX_VALUE, TimeRange.toEpochMs(Instant.MAX));

        Instant normalInstant = Instant.ofEpochMilli(1672531200000L);
        assertEquals(1672531200000L, TimeRange.toEpochMs(normalInstant));
    }

    @Test
    void splitWithResolution_dividesEvenly()
    {
        Instant fourHoursEnd = START.plus(Duration.ofHours(4));
        TimeRange range = new TimeRange(START, fourHoursEnd);
        Duration step = Duration.ofHours(1);

        List<TimeRange> slots = range.split(step);

        assertEquals(4, slots.size());

        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), slots.get(0).start());
        assertEquals(Instant.parse("2026-01-01T01:00:00Z"), slots.get(0).end());
        assertEquals(step, slots.get(0).resolution());

        assertEquals(Instant.parse("2026-01-01T01:00:00Z"), slots.get(1).start());
        assertEquals(Instant.parse("2026-01-01T02:00:00Z"), slots.get(1).end());

        assertEquals(Instant.parse("2026-01-01T02:00:00Z"), slots.get(2).start());
        assertEquals(Instant.parse("2026-01-01T03:00:00Z"), slots.get(2).end());

        assertEquals(Instant.parse("2026-01-01T03:00:00Z"), slots.get(3).start());
        assertEquals(Instant.parse("2026-01-01T04:00:00Z"), slots.get(3).end());
    }

    @Test
    void splitWithResolution_discardsRemainderWhenNotDivisible()
    {
        Instant fourHoursEnd = START.plus(Duration.ofHours(4));
        TimeRange range = new TimeRange(START, fourHoursEnd); // 4 hours total
        Duration step = Duration.ofHours(3);

        List<TimeRange> slots = range.split(step);

        // Only one full 3-hour slot fits (00:00 to 03:00); the 1-hour remainder (03:00 to 04:00) is discarded
        assertEquals(1, slots.size());
        assertEquals(START, slots.getFirst().start());
        assertEquals(Instant.parse("2026-01-01T03:00:00Z"), slots.getFirst().end());
        assertEquals(step, slots.getFirst().resolution());
    }

    @Test
    void splitWithResolution_equalDurationReturnsSingleSlot()
    {
        TimeRange range = new TimeRange(START, END); // 24 hours total
        Duration step = DURATION_ONE_DAY;

        List<TimeRange> slots = range.split(step);

        assertEquals(1, slots.size());
        assertEquals(START, slots.getFirst().start());
        assertEquals(END, slots.getFirst().end());
        assertEquals(step, slots.getFirst().resolution());
    }

    @Test
    void splitNoArgs_usesInstanceResolution()
    {
        Duration step = Duration.ofMinutes(30);
        TimeRange range = new TimeRange(START, START.plus(Duration.ofHours(2)), step);

        List<TimeRange> slots = range.split();

        assertEquals(4, slots.size());
        for (TimeRange slot : slots)
            assertEquals(step, slot.resolution());
        assertEquals(START, slots.get(0).start());
        assertEquals(START.plus(Duration.ofHours(2)), slots.get(3).end());
    }

    @Test
    void splitNoArgs_defaultResolutionReturnsSingleSlot()
    {
        TimeRange range = new TimeRange(START, END); // Default resolution equals 24 hours

        List<TimeRange> slots = range.split();

        assertEquals(1, slots.size());
        assertEquals(START, slots.getFirst().start());
        assertEquals(END, slots.getFirst().end());
        assertEquals(DURATION_ONE_DAY, slots.getFirst().resolution());
    }

    @Test
    void split_returnsUnmodifiableList()
    {
        TimeRange range = new TimeRange(START, END);
        List<TimeRange> slots = range.split(Duration.ofHours(1));

        TimeRange dummy = new TimeRange(START, START.plusSeconds(60));
        assertThrows(UnsupportedOperationException.class, () -> slots.add(dummy));
    }

    @Test
    void splitWithResolution_nullResolutionThrowsException()
    {
        TimeRange range = new TimeRange(START, END);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> range.split(null));
        assertEquals("Resolution cannot be null", exception.getMessage());
    }

    @Test
    void splitWithResolution_zeroOrNegativeResolutionThrowsException()
    {
        TimeRange range = new TimeRange(START, END);

        IllegalArgumentException zeroEx = assertThrows(IllegalArgumentException.class, () -> range.split(Duration.ZERO));
        assertEquals("Resolution must be strictly positive", zeroEx.getMessage());

        IllegalArgumentException negEx = assertThrows(IllegalArgumentException.class, () -> range.split(Duration.ofMinutes(-5)));
        assertEquals("Resolution must be strictly positive", negEx.getMessage());
    }

    @Test
    void splitWithResolution_resolutionLargerThanDurationThrowsException()
    {
        TimeRange range = new TimeRange(START, END); // 24 hours
        Duration tooLarge = Duration.ofHours(25);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> range.split(tooLarge));
        assertEquals("Resolution must be less than or equal to the total time range duration", exception.getMessage());
    }

    @Test
    void getCalendarFractionOfYear_normalYear()
    {
        // 2026 is not a leap year (365 days)
        Instant halfYearEnd = ZonedDateTime.of(2026, 7, 2, 12, 0, 0, 0, ZoneOffset.UTC).toInstant(); // Exactly 182.5 days later
        TimeRange range = new TimeRange(START, halfYearEnd);

        assertEquals(0.5, range.getCalendarFractionOfYear(), 0.0001);
    }

    @Test
    void getCalendarFractionOfYear_leapYear()
    {
        // 2024 is a leap year (366 days)
        Instant leapStart = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();

        // Jan(31) + Feb(29) + Mar(31) + Apr(30) + May(31) + Jun(30) = 182 days
        // To get exactly 183 days (half of 366), we need July 2nd at 00:00
        Instant leapHalfEnd = ZonedDateTime.of(2024, 7, 2, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();
        TimeRange range = new TimeRange(leapStart, leapHalfEnd);

        assertEquals(0.5, range.getCalendarFractionOfYear(), 0.0001);
    }

    @Test
    void getCalendarFractionOfYear_withInfiniteRange_throwsException()
    {
        TimeRange minRange = new TimeRange(Instant.MIN, END);
        assertThrows(IllegalArgumentException.class, minRange::getCalendarFractionOfYear);

        TimeRange maxRange = new TimeRange(START, Instant.MAX);
        assertThrows(IllegalArgumentException.class, maxRange::getCalendarFractionOfYear);
    }
}
