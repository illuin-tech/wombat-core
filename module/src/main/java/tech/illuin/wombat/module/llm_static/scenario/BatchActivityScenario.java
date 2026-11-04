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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Models scheduled batch workload activity with trigger time, duration, and optional smoothing,
 * with annual request volume distributed across active batch executions.
 *
 * @param outputTokenPerRequest mean output token count per request (>= 0)
 * @param requestPerYear total annual request volume (>= 0.0)
 * @param triggerTime daily start time of batch job
 * @param duration active execution duration
 * @param jitter deterministic variance factor (e.g. 0.05 for +-5%)
 * @param smoothing transition smoothing mode (none, linear ramp, smooth step)
 * @param applicability optional applicability filter
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BatchActivityScenario(
    @PositiveOrZero @JsonProperty("outputTokenPerRequest") @JsonAlias({"output-token-per-request", "output-token-count"}) int outputTokenPerRequest,
    @JsonProperty("requestPerYear") @JsonAlias({"request-per-year", "requestsPerYear", "peak-request-per-year", "peakRequestPerYear", "peakRequestsPerYear"}) @PositiveOrZero int requestPerYear,
    @NotNull @JsonProperty("triggerTime") @JsonAlias("trigger-time") LocalTime triggerTime,
    @NotNull @JsonProperty("duration") Duration duration,
    @JsonProperty("jitter") double jitter,
    @JsonProperty("smoothing") SmoothingMode smoothing,
    @JsonProperty("applicability") ApplicabilityFilter applicability
) implements Scenario
{
    public static final SmoothingMode DEFAULT_SMOOTHING = SmoothingMode.NONE;

    public BatchActivityScenario
    {
        smoothing = smoothing != null ? smoothing : DEFAULT_SMOOTHING;
        applicability = applicability != null ? applicability : ApplicabilityFilter.ALWAYS;
    }

    public BatchActivityScenario(int outputTokenPerRequest, int requestPerYear, LocalTime triggerTime, Duration duration, double jitter, SmoothingMode smoothing)
    {
        this(outputTokenPerRequest, requestPerYear, triggerTime, duration, jitter, smoothing, ApplicabilityFilter.ALWAYS);
    }

    public BatchActivityScenario(int outputTokenPerRequest, int requestPerYear, LocalTime triggerTime, Duration duration)
    {
        this(outputTokenPerRequest, requestPerYear, triggerTime, duration, 0.0, DEFAULT_SMOOTHING, ApplicabilityFilter.ALWAYS);
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

        ZoneId zone = dateTime.getZone() != null ? dateTime.getZone() : ZoneOffset.UTC;
        ZonedDateTime activeTrigger = this.resolveActiveTrigger(dateTime, zone);

        if (activeTrigger == null)
            return 0.0;

        double progress = this.computeProgress(activeTrigger, dateTime);
        double factor = this.smoothing.calculateFactor(progress);

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
        if (this.requestPerYear <= 0.0 || this.duration == null || this.duration.isZero() || this.duration.isNegative())
            return 0.0;

        double fDays = this.applicability.annualActiveFraction();
        if (fDays <= 0.0)
            return 0.0;

        double fBatch = (double) this.duration.toNanos() / (double) Duration.ofDays(1).toNanos();
        if (fBatch <= 0.0)
            return 0.0;

        double iSmoothing = this.smoothing.integralFactor();
        if (iSmoothing <= 0.0)
            return 0.0;

        double fActive = fDays * fBatch * iSmoothing;
        return fActive > 0.0 ? this.requestPerYear / fActive : 0.0;
    }

    private ZonedDateTime resolveActiveTrigger(ZonedDateTime localDateTime, ZoneId zone)
    {
        if (this.duration == null || this.duration.isZero() || this.duration.isNegative())
            return null;

        ZonedDateTime todayTrigger = localDateTime.toLocalDate().atTime(this.triggerTime).atZone(zone);
        if (!localDateTime.isBefore(todayTrigger) && !localDateTime.isAfter(todayTrigger.plus(this.duration)))
            return todayTrigger;

        ZonedDateTime yesterdayTrigger = todayTrigger.minusDays(1);
        if (!localDateTime.isBefore(yesterdayTrigger) && !localDateTime.isAfter(yesterdayTrigger.plus(this.duration)))
            return yesterdayTrigger;

        return null;
    }

    private double computeProgress(ZonedDateTime trigger, ZonedDateTime current)
    {
        Duration total = this.duration;
        if (total == null || total.isZero() || total.isNegative())
            return 1.0;

        Duration elapsed = Duration.between(trigger, current);
        return (double) elapsed.toNanos() / (double) total.toNanos();
    }

    public enum SmoothingMode
    {
        @JsonProperty("none")
        NONE,
        @JsonProperty("linear_ramp")
        LINEAR_RAMP,
        @JsonProperty("smooth_step")
        SMOOTH_STEP;

        public double calculateFactor(double progress)
        {
            if (progress < 0.0 || progress > 1.0)
                return 0.0;

            return switch (this)
            {
                case NONE -> 1.0;
                case LINEAR_RAMP -> {
                    double ramp = 0.1;
                    if (progress < ramp)
                        yield progress / ramp;
                    if (progress <= 1.0 - ramp)
                        yield 1.0;
                    yield (1.0 - progress) / ramp;
                }
                case SMOOTH_STEP -> {
                    double ramp = 0.1;
                    if (progress < ramp)
                    {
                        double x = progress / ramp;
                        yield x * x * (3.0 - 2.0 * x);
                    }
                    if (progress <= 1.0 - ramp)
                        yield 1.0;
                    double x = (1.0 - progress) / ramp;
                    yield x * x * (3.0 - 2.0 * x);
                }
            };
        }

        public double integralFactor()
        {
            return switch (this)
            {
                case NONE -> 1.0;
                case LINEAR_RAMP -> 0.9;
                case SMOOTH_STEP -> 0.9;
            };
        }
    }
}
