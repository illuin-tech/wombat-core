package tech.illuin.wombat.core.activity.commons;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public record TimeRange(
    @JsonProperty("start") Instant start,
    @JsonProperty("end") Instant end
) {
    public static long toEpochMs(Instant instant)
    {
        if (instant.equals(Instant.MIN))
            return Long.MIN_VALUE;
        if (instant.equals(Instant.MAX))
            return Long.MAX_VALUE;
        return instant.toEpochMilli();
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
