package tech.illuin.wombat.module.llm_static.scenario;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.PositiveOrZero;
import tech.illuin.wombat.module.llm_static.scenario.tools.ApplicabilityFilter;

import java.time.ZonedDateTime;

/**
 * Models a constant traffic level.
 */
@JsonClassDescription("Constant traffic")
public record ConstantScenario(
    @PositiveOrZero @JsonProperty(defaultValue = "1.0") @JsonPropertyDescription("Relative traffic level, only its ratio to the model's other scenarios matters") Double level,
    @JsonPropertyDescription("Days and months the scenario applies to, always when omitted") ApplicabilityFilter applicability
) implements Scenario
{
    public ConstantScenario
    {
        level = level != null ? level : DEFAULT_LEVEL;
        applicability = applicability != null ? applicability : ApplicabilityFilter.ALWAYS;
    }

    public ConstantScenario(double level)
    {
        this(level, null);
    }

    public ConstantScenario()
    {
        this(DEFAULT_LEVEL, null);
    }

    @Override
    public boolean isApplicable(ZonedDateTime dateTime)
    {
        return this.applicability.isApplicable(dateTime);
    }

    @Override
    public double shape(ZonedDateTime dateTime)
    {
        return this.isApplicable(dateTime) ? this.level : 0.0;
    }
}
