package tech.illuin.wombat.module.llm_static.scenario;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.ZonedDateTime;

/**
 * One curve of a model's traffic shape. The model's scenarios draw its shape, the busiest one winning at each instant,
 * and the model's annual volume sets the scale.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ConstantScenario.class, name = "constant"),
    @JsonSubTypes.Type(value = UserActivityScenario.class, name = "user_activity"),
    @JsonSubTypes.Type(value = BatchActivityScenario.class, name = "batch_activity")
})
public interface Scenario
{
    /** The level a scenario has when none is given. */
    double DEFAULT_LEVEL = 1.0;

    boolean isApplicable(ZonedDateTime dateTime);

    /**
     * The scenario's height at the given instant: from 0 to its level, 0 when it does not apply.
     *
     * @param dateTime evaluation instant
     * @return relative traffic, only its ratio to the model's other scenarios matters
     */
    double shape(ZonedDateTime dateTime);
}
