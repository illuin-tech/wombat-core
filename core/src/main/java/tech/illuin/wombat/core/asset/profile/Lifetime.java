package tech.illuin.wombat.core.asset.profile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Lifetime(
    long value,
    Unit unit
) {
    public static final Lifetime ZERO = new Lifetime(0, Unit.DAYS);
    public static final Lifetime ONE_HOUR = new Lifetime(1, Unit.HOURS);
    public static final Lifetime ONE_DAY = new Lifetime(1, Unit.DAYS);
    public static final Lifetime ONE_MONTH = new Lifetime(1, Unit.MONTHS);
    public static final Lifetime ONE_YEAR = new Lifetime(1, Unit.YEARS);

    private static final Pattern FORMAT_PATTERN = Pattern.compile("^(\\d+)(?:\\s*([dmyh]))?$", Pattern.CASE_INSENSITIVE);
    private static final long HOURS_PER_DAY = 24L;
    private static final long HOURS_PER_YEAR = 365L * HOURS_PER_DAY;
    private static final long HOURS_PER_MONTH = HOURS_PER_YEAR / 12L;

    public enum Unit
    {
        HOURS,
        DAYS,
        MONTHS,
        YEARS,
    }

    public static Lifetime of(long value, Unit unit)
    {
        return new Lifetime(value, unit);
    }

    public static Lifetime ofHours(long value)
    {
        return new Lifetime(value, Unit.HOURS);
    }

    public static Lifetime ofDays(long value)
    {
        return new Lifetime(value, Unit.DAYS);
    }

    public static Lifetime ofMonths(long value)
    {
        return new Lifetime(value, Unit.MONTHS);
    }

    public static Lifetime ofYears(long value)
    {
        return new Lifetime(value, Unit.YEARS);
    }

    public long asHours()
    {
        return switch (this.unit) {
            case HOURS -> this.value;
            case DAYS -> this.value * HOURS_PER_DAY;
            case MONTHS -> this.value * HOURS_PER_MONTH;
            case YEARS -> this.value * HOURS_PER_YEAR;
        };
    }

    public Duration asDuration()
    {
        return Duration.ofHours(this.asHours());
    }

    public Lifetime plus(Lifetime other)
    {
        return new Lifetime(this.value + other.value, this.unit);
    }

    public Lifetime multiply(long factor)
    {
        return new Lifetime(this.value * factor, this.unit);
    }

    @JsonCreator
    public static Lifetime parse(long hours)
    {
        return ofHours(hours);
    }

    @JsonCreator
    public static Lifetime parse(String text)
    {
        if (text == null || text.isBlank())
            throw new IllegalArgumentException("A Lifetime string cannot be null or blank");

        Matcher matcher = FORMAT_PATTERN.matcher(text.trim());
        if (!matcher.matches())
            throw new IllegalArgumentException("Invalid lifetime format: '" + text + "'. Expected format like '5y', '60m', '1825d', or bare hours like '43800'");

        long amount = Long.parseLong(matcher.group(1));
        String suffix = matcher.group(2);
        if (suffix == null || suffix.isBlank())
            return ofHours(amount);

        Unit unit = switch (suffix.toLowerCase()) {
            case "h" -> Unit.HOURS;
            case "d" -> Unit.DAYS;
            case "m" -> Unit.MONTHS;
            case "y" -> Unit.YEARS;
            default -> throw new IllegalArgumentException("Unknown unit suffix: " + suffix);
        };

        return new Lifetime(amount, unit);
    }

    @JsonValue
    public String asText()
    {
        String suffix = switch (this.unit) {
            case HOURS -> "h";
            case DAYS -> "d";
            case MONTHS -> "m";
            case YEARS -> "y";
        };
        return this.value + suffix;
    }
}
