package tech.illuin.wombat.core.activity.commons;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record TimeRange(
    @JsonProperty("start") Instant start,
    @JsonProperty("end") Instant end
) {
    public static long toEpochMs(Instant instant)
    {
        if (instant.equals(Instant.MIN)) return Long.MIN_VALUE;
        if (instant.equals(Instant.MAX)) return Long.MAX_VALUE;
        return instant.toEpochMilli();
    }
}
