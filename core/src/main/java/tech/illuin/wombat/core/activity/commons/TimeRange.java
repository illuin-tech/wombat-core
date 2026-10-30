package tech.illuin.wombat.core.activity.commons;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record TimeRange(
    @JsonProperty("start") Instant start,
    @JsonProperty("end") Instant end,
    @JsonProperty("resolution") Duration resolution
) {
    public TimeRange
    {
        if (Duration.between(start, end).compareTo(resolution) < 0)
            throw new IllegalArgumentException("Resolution must be less than or equal to the interval defined by the start and end timestamps.");
    }

    public TimeRange(Instant start, Instant end)
    {
        this(start, end, Duration.between(start, end));
    }

    public static TimeRange between(Instant start, Instant end)
    {
        return new TimeRange(start, end);
    }

    public static TimeRange clone(TimeRange original)
    {
        return new TimeRange(original.start(), original.end(), original.resolution());
    }

    public static TimeRange cloneRescale(TimeRange original, Duration resolution)
    {
        return new TimeRange(original.start(), original.end(), resolution);
    }

    public static TimeRange cloneRescale(TimeRange original)
    {
        return new TimeRange(original.start(), original.end());
    }

    public static long toEpochMs(Instant instant)
    {
        if (instant.equals(Instant.MIN))
            return Long.MIN_VALUE;
        if (instant.equals(Instant.MAX))
            return Long.MAX_VALUE;
        return instant.toEpochMilli();
    }

    /**
     * Split the time range into smaller time ranges of the given resolution.
     * If the resolution is not a multiple of the time range duration, the remainder will be discarded.
     *
     * @param resolution
     * @return
     */
    public List<TimeRange> split(Duration resolution)
    {
        if (resolution == null)
            throw new IllegalArgumentException("Resolution cannot be null");
        if (resolution.isZero() || resolution.isNegative())
            throw new IllegalArgumentException("Resolution must be strictly positive");
        if (Duration.between(this.start, this.end).compareTo(resolution) < 0)
            throw new IllegalArgumentException("Resolution must be less than or equal to the total time range duration");

        List<TimeRange> slots = new ArrayList<>();
        Instant current = this.start;

        while (!current.plus(resolution).isAfter(this.end))
        {
            Instant next = current.plus(resolution);
            slots.add(new TimeRange(current, next, resolution));
            current = next;
        }

        return Collections.unmodifiableList(slots);
    }

    /**
     * Splits this time range into consecutive sub-ranges using this instance's resolution.
     *
     * @return an unmodifiable list of consecutive {@link TimeRange} slots
     */
    public List<TimeRange> split()
    {
        return split(this.resolution);
    }

    public double getCalendarFractionOfYear()
    {
        if (this.start.equals(Instant.MIN) || this.end.equals(Instant.MAX))
            throw new IllegalArgumentException("Cannot calculate fraction of a year for an infinite time range.");

        ZonedDateTime startZone = this.start.atZone(ZoneOffset.UTC);
        ZonedDateTime endZone = this.end.atZone(ZoneOffset.UTC);

        double durationMs = ChronoUnit.MILLIS.between(startZone, endZone);
        int daysInYear = startZone.toLocalDate().lengthOfYear();
        double msInCalendarYear = daysInYear * 24 * 60 * 60 * 1000L;
        return durationMs / msInCalendarYear;
    }
}
