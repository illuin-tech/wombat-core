package tech.illuin.wombat.module.kubernetes_static.scenario;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import tech.illuin.wombat.module.kubernetes_static.scenario.tools.ApplicabilityFilter;
import tech.illuin.wombat.module.kubernetes_static.scenario.tools.DeterministicJitter;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Models scheduled batch workload activity with trigger time, duration, and optional smoothing,
 * with baseline and peak load expressed as fractions of 100% server capacity
 * (e.g. {@code 0.0} baseline, {@code 0.90} peak batch load).
 *
 * @param baselineLoad fraction of server capacity outside batch window (>= 0.0)
 * @param peakLoad fraction of server capacity during batch execution (>= 0.0)
 * @param triggerTime daily start time of batch job
 * @param duration active execution duration
 * @param jitter deterministic variance factor (e.g. 0.05 for +-5%)
 * @param smoothing transition smoothing mode (none, linear ramp, smooth step)
 * @param applicability optional applicability filter
 */
public record BatchActivityScenario(
    @PositiveOrZero @JsonProperty("baselineLoad") @JsonAlias("baseline-load") double baselineLoad,
    @PositiveOrZero @JsonProperty("peakLoad") @JsonAlias("peak-load") double peakLoad,
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

    public BatchActivityScenario(
        double baselineLoad,
        double peakLoad,
        LocalTime triggerTime,
        Duration duration,
        double jitter,
        SmoothingMode smoothing
    ) {
        this(baselineLoad, peakLoad, triggerTime, duration, jitter, smoothing, null);
    }

    @Override
    public boolean isApplicable(ZonedDateTime dateTime)
    {
        return this.applicability.isApplicable(dateTime);
    }

    @Override
    public double evaluateLoad(ZonedDateTime dateTime)
    {
        if (dateTime == null)
            return 0.0;

        ZoneId zone = this.applicability.zoneId() != null
            ? this.applicability.zoneId()
            : (dateTime.getZone() != null ? dateTime.getZone() : ZoneOffset.UTC);

        ZonedDateTime localDateTime = dateTime.withZoneSameInstant(zone);
        ZonedDateTime activeTrigger = this.resolveActiveTrigger(localDateTime, zone);

        if (activeTrigger == null)
            return this.baselineLoad;

        double progress = this.computeProgress(activeTrigger, localDateTime);
        double factor = this.smoothing.calculateFactor(progress);

        double rawLoad = this.baselineLoad + (this.peakLoad - this.baselineLoad) * factor;
        return DeterministicJitter.apply(rawLoad, this.jitter, localDateTime);
    }

    private ZonedDateTime resolveActiveTrigger(ZonedDateTime localDateTime, ZoneId zone)
    {
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
        if (total.isZero() || total.isNegative())
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
    }
}
