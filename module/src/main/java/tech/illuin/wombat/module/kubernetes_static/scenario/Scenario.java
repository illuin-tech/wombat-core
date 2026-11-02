package tech.illuin.wombat.module.kubernetes_static.scenario;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.ZonedDateTime;

/**
 * Represents a static load modeling scenario.
 * <p>
 * Load values returned by {@link #evaluateLoad(ZonedDateTime)} represent fractions of 100% server capacity
 * in {@code [0.0, 1.0]} (e.g. {@code 0.10} for 10% load, {@code 0.75} for 75% load, {@code 1.0} for 100% capacity).
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
     * Evaluates the load factor at the given date/time as a fraction of 100% server capacity.
     *
     * @param dateTime evaluation instant
     * @return load factor as a fraction of server capacity
     */
    double evaluateLoad(ZonedDateTime dateTime);
}
