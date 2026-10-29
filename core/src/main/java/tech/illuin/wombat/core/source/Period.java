package tech.illuin.wombat.core.source;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a data type used for describing Period that conform to timescales that are compatible with the wombat monitor (hence why we didn't simply use java.time.Period or java.time.Duration)
 *
 * @param value
 * @param unit
 */
public record Period(
    long value,
    Unit unit
) {
    public static final Period ZERO = new Period(0, Unit.SECONDS);
    public static final Period ONE_SECOND = new Period(1, Unit.SECONDS);
    public static final Period ONE_MINUTE = new Period(1, Unit.MINUTES);
    public static final Period ONE_HOUR = new Period(1, Unit.HOURS);

    private static final Pattern FORMAT_PATTERN = Pattern.compile("^(\\d+)\\s*([smh])$", Pattern.CASE_INSENSITIVE);

    public enum Unit
    {
        SECONDS,
        MINUTES,
        HOURS,
    }

    public static Period of(long value, Unit unit)
    {
        return new Period(value, unit);
    }

    public static Period ofSeconds(long value)
    {
        return new Period(value, Unit.SECONDS);
    }

    public static Period ofMinutes(long value)
    {
        return new Period(value, Unit.MINUTES);
    }

    public static Period ofHours(long value)
    {
        return new Period(value, Unit.HOURS);
    }

    public Duration asDuration()
    {
        return switch (this.unit) {
            case SECONDS -> Duration.ofSeconds(value);
            case MINUTES -> Duration.ofMinutes(value);
            case HOURS -> Duration.ofHours(value);
        };
    }

    public Period plus(Period other)
    {
        return new Period(this.value + other.value, this.unit);
    }

    public Period multiply(long factor)
    {
        return new Period(this.value * factor, this.unit);
    }

    @JsonCreator
    public static Period parse(String text)
    {
        if (text == null || text.isBlank())
            throw new IllegalArgumentException("A Period string cannot be null or blank");

        Matcher matcher = FORMAT_PATTERN.matcher(text.trim());
        if (!matcher.matches())
            throw new IllegalArgumentException("Invalid period format: '" + text + "'. Expected format like '1h', '2m', '3s'");

        long amount = Long.parseLong(matcher.group(1));
        String suffix = matcher.group(2).toLowerCase();
        Unit unit = switch (suffix) {
            case "s" -> Unit.SECONDS;
            case "m" -> Unit.MINUTES;
            case "h" -> Unit.HOURS;
            default -> throw new IllegalArgumentException("Unknown unit suffix: " + suffix);
        };

        return new Period(amount, unit);
    }

    /**
     * Serializes the record into shorthand string format (e.g., "1h").
     */
    @JsonValue
    public String asText()
    {
        String suffix = switch (this.unit) {
            case SECONDS -> "s";
            case MINUTES -> "m";
            case HOURS -> "h";
        };
        return this.value + suffix;
    }
}
