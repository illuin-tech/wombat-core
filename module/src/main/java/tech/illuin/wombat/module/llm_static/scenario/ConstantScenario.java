package tech.illuin.wombat.module.llm_static.scenario;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.PositiveOrZero;
import tech.illuin.wombat.module.llm_static.scenario.tools.ApplicabilityFilter;

import java.time.ZonedDateTime;

/**
 * Models a constant baseline annual request count and output token count per request.
 *
 * @param outputTokenPerRequest mean output token count per request (>= 0)
 * @param requestPerYear annual request count (>= 0.0)
 * @param applicability optional applicability filter
 */
public record ConstantScenario(
    @PositiveOrZero @JsonProperty("outputTokenPerRequest") @JsonAlias({"output-token-per-request", "output-token-count"}) int outputTokenPerRequest,
    @PositiveOrZero @JsonProperty("requestPerYear") @JsonAlias({"request-per-year", "requestsPerYear"}) double requestPerYear,
    @JsonProperty("applicability") ApplicabilityFilter applicability
) implements Scenario
{
    public ConstantScenario
    {
        applicability = applicability != null ? applicability : ApplicabilityFilter.ALWAYS;
    }

    public ConstantScenario(int outputTokenPerRequest, double requestPerYear)
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
    public int outputTokenPerRequest()
    {
        return this.outputTokenPerRequest;
    }
}
