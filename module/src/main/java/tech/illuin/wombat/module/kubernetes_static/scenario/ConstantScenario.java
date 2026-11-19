package tech.illuin.wombat.module.kubernetes_static.scenario;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.PositiveOrZero;
import tech.illuin.wombat.module.kubernetes_static.scenario.tools.ApplicabilityFilter;

import java.time.ZonedDateTime;

/**
 * Models a constant baseline load factor expressed as a fraction of 100% server capacity
 * (e.g. {@code 0.10} for 10% load, {@code 1.0} for 100% capacity).
 */
@JsonClassDescription("Constant load")
public record ConstantScenario(
    @PositiveOrZero @JsonPropertyDescription("Fraction of the server capacity, e.g. 0.20 for 20%") double load,
    @JsonPropertyDescription("Days and months the scenario applies to, always when omitted") ApplicabilityFilter applicability
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
