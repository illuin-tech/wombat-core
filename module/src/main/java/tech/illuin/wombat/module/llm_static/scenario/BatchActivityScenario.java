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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Models a scheduled daily batch job, at the scenario's level over its window, with optional smoothing at both ends.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonClassDescription("Traffic of a scheduled daily batch job")
public record BatchActivityScenario(
    @PositiveOrZero @JsonProperty(defaultValue = "1.0") @JsonPropertyDescription("Relative traffic level over the batch window, only its ratio to the model's other scenarios matters") Double level,
    @NotNull @JsonPropertyDescription("Daily start time of the batch job") LocalTime triggerTime,
    @NotNull @JsonPropertyDescription("Duration of the batch job (ISO-8601)") Duration duration,
    @JsonProperty(defaultValue = "0") @JsonPropertyDescription("Deterministic variance factor, e.g. 0.05 for +-5%") double jitter,
    @JsonProperty(defaultValue = "none") @JsonPropertyDescription("Smoothing of the transitions at both ends of the batch window") SmoothingMode smoothing,
    @JsonPropertyDescription("Days and months the scenario applies to, always when omitted") ApplicabilityFilter applicability
) implements Scenario
{
    public static final SmoothingMode DEFAULT_SMOOTHING = SmoothingMode.NONE;

    public BatchActivityScenario
    {
        level = level != null ? level : DEFAULT_LEVEL;
        smoothing = smoothing != null ? smoothing : DEFAULT_SMOOTHING;
        applicability = applicability != null ? applicability : ApplicabilityFilter.ALWAYS;
    }

    public BatchActivityScenario(double level, LocalTime triggerTime, Duration duration, SmoothingMode smoothing)
    {
        this(level, triggerTime, duration, 0.0, smoothing, ApplicabilityFilter.ALWAYS);
    }

    public BatchActivityScenario(double level, LocalTime triggerTime, Duration duration)
    {
        this(level, triggerTime, duration, 0.0, DEFAULT_SMOOTHING, ApplicabilityFilter.ALWAYS);
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

        ZoneId zone = dateTime.getZone() != null ? dateTime.getZone() : ZoneOffset.UTC;
        ZonedDateTime activeTrigger = this.resolveActiveTrigger(dateTime, zone);

        if (activeTrigger == null)
            return 0.0;

        double progress = this.computeProgress(activeTrigger, dateTime);
        return DeterministicJitter.apply(this.level * this.smoothing.calculateFactor(progress), this.jitter, dateTime);
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
    }
}
