package tech.illuin.wombat.module.kubernetes_static.scenario;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.PositiveOrZero;
import tech.illuin.wombat.module.kubernetes_static.scenario.tools.ApplicabilityFilter;

import java.time.ZonedDateTime;

/**
 * Models a constant baseline load factor expressed as a fraction of 100% server capacity
 * (e.g. {@code 0.10} for 10% load, {@code 1.0} for 100% capacity).
 *
 * @param load fraction of server capacity (>= 0.0)
 * @param applicability optional applicability filter
 */
public record ConstantScenario(
    @PositiveOrZero @JsonProperty("load") double load,
    @JsonProperty("applicability") ApplicabilityFilter applicability
) implements Scenario
{
    public ConstantScenario
    {
        applicability = applicability != null ? applicability : ApplicabilityFilter.ALWAYS;
    }

    public ConstantScenario(double load)
    {
        this(load, null);
    }

    @Override
    public boolean isApplicable(ZonedDateTime dateTime)
    {
        return this.applicability.isApplicable(dateTime);
    }

    @Override
    public double evaluateLoad(ZonedDateTime dateTime)
    {
        return this.load;
    }
}
