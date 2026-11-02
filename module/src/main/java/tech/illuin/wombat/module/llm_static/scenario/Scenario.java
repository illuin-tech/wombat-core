package tech.illuin.wombat.module.llm_static.scenario;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.ZonedDateTime;

/**
 * Represents a static LLM activity modeling scenario.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ConstantScenario.class, name = "constant"),
    @JsonSubTypes.Type(value = UserActivityScenario.class, name = "user_activity"),
    @JsonSubTypes.Type(value = BatchActivityScenario.class, name = "batch_activity")
})
public interface Scenario
{
    boolean isApplicable(ZonedDateTime dateTime);

    /**
     * Evaluates the annual request count rate at the given date/time.
     *
     * @param dateTime evaluation instant
     * @return annual request count rate
     */
    double evaluateRequestPerYear(ZonedDateTime dateTime);

    /**
     * Returns the mean output token count per request for this scenario.
     *
     * @return mean output token count per request
     */
    int outputTokenPerRequest();
}
