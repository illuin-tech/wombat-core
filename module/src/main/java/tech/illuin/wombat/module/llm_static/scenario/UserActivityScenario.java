package tech.illuin.wombat.module.llm_static.scenario;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
 * with annual request volume distributed across active periods and modulated by mathematical curves.
 *
 * @param outputTokenPerRequest mean output token count per request (>= 0)
 * @param requestPerYear total annual request volume (>= 0.0)
 * @param openingTime start time of user activity window
 * @param closingTime end time of user activity window
 * @param curve mathematical curve profile (sinusoidal, trapezoidal, bell)
 * @param jitter deterministic variance factor (e.g. 0.05 for +-5%)
 * @param applicability optional applicability filter
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserActivityScenario(
    @PositiveOrZero @JsonProperty("outputTokenPerRequest") @JsonAlias({"output-token-per-request", "output-token-count"}) int outputTokenPerRequest,
    @JsonProperty("requestPerYear") @JsonAlias({"request-per-year", "requestsPerYear", "peak-request-per-year", "peakRequestPerYear", "peakRequestsPerYear"}) @PositiveOrZero int requestPerYear,
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

    public UserActivityScenario(
        int outputTokenPerRequest,
        int requestPerYear,
        LocalTime openingTime,
        LocalTime closingTime,
        CurveShape curve,
        double jitter
    ) {
        this(outputTokenPerRequest, requestPerYear, openingTime, closingTime, curve, jitter, ApplicabilityFilter.ALWAYS);
    }

    @Override
    public boolean isApplicable(ZonedDateTime dateTime)
    {
        return this.applicability.isApplicable(dateTime);
    }

    @Override
    public double evaluateRequestPerYear(ZonedDateTime dateTime)
    {
        if (dateTime == null || !this.isApplicable(dateTime))
            return 0.0;

        double progress = this.computeProgress(dateTime.toLocalTime());
        if (progress <= 0.0 || progress >= 1.0)
            return 0.0;

        double factor = this.curve.calculateFactor(progress);
        double rawRequest = this.calculatePeakRate() * factor;
        return DeterministicJitter.apply(rawRequest, this.jitter, dateTime);
    }

    @Override
    public @PositiveOrZero int outputTokenPerRequest()
    {
        return this.outputTokenPerRequest;
    }

    private double calculatePeakRate()
    {
        if (this.requestPerYear <= 0.0 || this.openingTime == null || this.closingTime == null || this.openingTime.equals(this.closingTime))
            return 0.0;

        double fDays = this.applicability.annualActiveFraction();
        if (fDays <= 0.0)
            return 0.0;

        Duration windowDuration = this.openingTime.isBefore(this.closingTime)
            ? Duration.between(this.openingTime, this.closingTime)
            : Duration.ofDays(1).minus(Duration.between(this.closingTime, this.openingTime));

        double fWindow = (double) windowDuration.toNanos() / (double) Duration.ofDays(1).toNanos();
        if (fWindow <= 0.0)
            return 0.0;

        double iCurve = this.curve.integralFactor();
        if (iCurve <= 0.0)
            return 0.0;

        double fActive = fDays * fWindow * iCurve;
        return fActive > 0.0 ? this.requestPerYear / fActive : 0.0;
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

        public double integralFactor()
        {
            return switch (this)
            {
                case SINUSOIDAL -> 2.0 / Math.PI;
                case TRAPEZOIDAL -> 0.8;
                case BELL -> 0.5;
            };
        }
    }
}
