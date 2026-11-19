package tech.illuin.wombat.module.llm_static.scenario;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.PositiveOrZero;
import tech.illuin.wombat.module.llm_static.scenario.tools.ApplicabilityFilter;

import java.time.ZonedDateTime;

/**
 * Models a constant baseline annual request count and output token count per request.
 */
@JsonClassDescription("Constant request volume")
public record ConstantScenario(
    // Legacy names, still found in configuration files
    @PositiveOrZero @JsonPropertyDescription("Mean output tokens per request") @JsonAlias("output-token-count") int outputTokenPerRequest,
    @PositiveOrZero @JsonPropertyDescription("Annual request count") @JsonAlias("requestsPerYear") int requestPerYear,
    @JsonPropertyDescription("Days and months the scenario applies to, always when omitted") ApplicabilityFilter applicability
) implements Scenario
{
    public ConstantScenario
    {
        applicability = applicability != null ? applicability : ApplicabilityFilter.ALWAYS;
    }

    public ConstantScenario(int outputTokenPerRequest, int requestPerYear)
    {
        this(outputTokenPerRequest, requestPerYear, null);
    }

    @Override
    public boolean isApplicable(ZonedDateTime dateTime)
    {
        return this.applicability.isApplicable(dateTime);
    }

    @Override
    public double evaluateRequestPerYear(ZonedDateTime dateTime)
    {
        return this.requestPerYear;
    }

    @Override
    public @PositiveOrZero int outputTokenPerRequest()
    {
        return this.outputTokenPerRequest;
    }
}
