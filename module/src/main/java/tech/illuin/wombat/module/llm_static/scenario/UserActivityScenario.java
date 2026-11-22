package tech.illuin.wombat.module.llm_static.scenario;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import tech.illuin.wombat.module.llm_static.scenario.tools.ApplicabilityFilter;
import tech.illuin.wombat.module.llm_static.scenario.tools.DeterministicJitter;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/**
 * Models user activity between opening and closing times, following a curve that peaks at the scenario's level.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonClassDescription("Traffic following user activity over a daily window")
public record UserActivityScenario(
    @PositiveOrZero @JsonProperty(defaultValue = "1.0") @JsonPropertyDescription("Relative traffic level at the curve's peak, only its ratio to the model's other scenarios matters") Double level,
    @NotNull @JsonPropertyDescription("Start of the daily activity window") LocalTime openingTime,
    @NotNull @JsonPropertyDescription("End of the daily activity window") LocalTime closingTime,
    @JsonProperty(defaultValue = "bell") @JsonPropertyDescription("Shape of the activity over the window") CurveShape curve,
    @JsonProperty(defaultValue = "0") @JsonPropertyDescription("Deterministic variance factor, e.g. 0.05 for +-5%") double jitter,
    @JsonPropertyDescription("Days and months the scenario applies to, always when omitted") ApplicabilityFilter applicability
) implements Scenario
{
    public static final CurveShape DEFAULT_CURVE = CurveShape.BELL;

    public UserActivityScenario
    {
        level = level != null ? level : DEFAULT_LEVEL;
        curve = curve != null ? curve : DEFAULT_CURVE;
        applicability = applicability != null ? applicability : ApplicabilityFilter.ALWAYS;
    }

    public UserActivityScenario(double level, LocalTime openingTime, LocalTime closingTime, CurveShape curve)
    {
        this(level, openingTime, closingTime, curve, 0.0, ApplicabilityFilter.ALWAYS);
    }

    @Override
    public boolean isApplicable(ZonedDateTime dateTime)
    {
        return this.applicability.isApplicable(dateTime);
    }

    @Override
    public double shape(ZonedDateTime dateTime)
    {
        if (dateTime == null || !this.isApplicable(dateTime))
            return 0.0;

        double progress = this.computeProgress(dateTime.toLocalTime());
        if (progress <= 0.0 || progress >= 1.0)
            return 0.0;

        return DeterministicJitter.apply(this.level * this.curve.calculateFactor(progress), this.jitter, dateTime);
    }

    private double computeProgress(LocalTime time)
    {
        if (this.openingTime.equals(this.closingTime))
            return 0.0;

        if (this.openingTime.isBefore(this.closingTime))
        {
            if (time.isBefore(this.openingTime) || time.isAfter(this.closingTime))
                return 0.0;

            Duration total = Duration.between(this.openingTime, this.closingTime);
            Duration elapsed = Duration.between(this.openingTime, time);
            return (double) elapsed.toNanos() / (double) total.toNanos();
        }

        if (time.isAfter(this.closingTime) && time.isBefore(this.openingTime))
            return 0.0;

        Duration total = Duration.ofDays(1).minus(Duration.between(this.closingTime, this.openingTime));
        Duration elapsed = time.isBefore(this.openingTime)
            ? Duration.ofDays(1).minus(Duration.between(time, this.openingTime))
            : Duration.between(this.openingTime, time);

        return (double) elapsed.toNanos() / (double) total.toNanos();
    }

    public enum CurveShape
    {
        @JsonProperty("sinusoidal")
        SINUSOIDAL,
        @JsonProperty("trapezoidal")
        TRAPEZOIDAL,
        @JsonProperty("bell")
        BELL;

        public double calculateFactor(double progress)
        {
            if (progress <= 0.0 || progress >= 1.0)
                return 0.0;

            return switch (this)
            {
                case SINUSOIDAL -> Math.sin(Math.PI * progress);
                case TRAPEZOIDAL -> {
                    double ramp = 0.2;
                    if (progress < ramp)
                        yield progress / ramp;
                    if (progress <= 1.0 - ramp)
                        yield 1.0;
                    yield (1.0 - progress) / ramp;
                }
                case BELL -> 0.5 * (1.0 - Math.cos(2.0 * Math.PI * progress));
            };
        }
    }
}
