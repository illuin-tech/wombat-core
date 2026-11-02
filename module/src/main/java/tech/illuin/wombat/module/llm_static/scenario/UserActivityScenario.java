package tech.illuin.wombat.module.llm_static.scenario;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import tech.illuin.wombat.module.llm_static.scenario.tools.ApplicabilityFilter;
import tech.illuin.wombat.module.llm_static.scenario.tools.DeterministicJitter;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/**
 * Models user activity between opening and closing times with configurable curve shape,
 * with baseline and peak annual request counts modulated by mathematical curves.
 *
 * @param outputTokenPerRequest mean output token count per request (>= 0)
 * @param baselineRequestPerYear annual request count during off-hours (>= 0.0)
 * @param peakRequestPerYear annual request count at peak hours (>= 0.0)
 * @param openingTime start time of user activity window
 * @param closingTime end time of user activity window
 * @param curve mathematical curve profile (sinusoidal, trapezoidal, bell)
 * @param jitter deterministic variance factor (e.g. 0.05 for +-5%)
 * @param applicability optional applicability filter
 */
public record UserActivityScenario(
    @PositiveOrZero @JsonProperty("outputTokenPerRequest") @JsonAlias({"output-token-per-request", "output-token-count"}) int outputTokenPerRequest,
    @PositiveOrZero @JsonProperty("baselineRequestPerYear") @JsonAlias({"baseline-request-per-year", "baselineRequestsPerYear"}) double baselineRequestPerYear,
    @PositiveOrZero @JsonProperty("peakRequestPerYear") @JsonAlias({"peak-request-per-year", "peakRequestsPerYear"}) double peakRequestPerYear,
    @NotNull @JsonProperty("openingTime") @JsonAlias("opening-time") LocalTime openingTime,
    @NotNull @JsonProperty("closingTime") @JsonAlias("closing-time") LocalTime closingTime,
    @JsonProperty("curve") CurveShape curve,
    @JsonProperty("jitter") double jitter,
    @JsonProperty("applicability") ApplicabilityFilter applicability
) implements Scenario
{
    public static final CurveShape DEFAULT_CURVE = CurveShape.BELL;

    public UserActivityScenario
    {
        curve = curve != null ? curve : DEFAULT_CURVE;
        applicability = applicability != null ? applicability : ApplicabilityFilter.ALWAYS;
    }

    @Override
    public boolean isApplicable(ZonedDateTime dateTime)
    {
        return this.applicability.isApplicable(dateTime);
    }

    @Override
    public double evaluateRequestPerYear(ZonedDateTime dateTime)
    {
        if (dateTime == null)
            return 0.0;

        ZonedDateTime localDateTime = this.applicability.zoneId() != null
            ? dateTime.withZoneSameInstant(this.applicability.zoneId())
            : dateTime;

        double progress = this.computeProgress(localDateTime.toLocalTime());
        double factor = this.curve.calculateFactor(progress);

        double rawRequest = this.baselineRequestPerYear + (this.peakRequestPerYear - this.baselineRequestPerYear) * factor;
        return DeterministicJitter.apply(rawRequest, this.jitter, localDateTime);
    }

    @Override
    public int outputTokenPerRequest()
    {
        return this.outputTokenPerRequest;
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
