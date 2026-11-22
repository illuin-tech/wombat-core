package tech.illuin.wombat.module.llm_static.scenario.evaluator;

import tech.illuin.wombat.module.llm_static.scenario.Scenario;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * The scenarios model the behaviour of one model, not workloads to add up: at each instant, the busiest applicable
 * scenario draws the shape.
 */
public class CompositeScenarioEvaluator implements ScenarioEvaluator
{
    @Override
    public double evaluate(ZonedDateTime dateTime, List<Scenario> scenarios)
    {
        if (scenarios == null || scenarios.isEmpty() || dateTime == null)
            return 0.0;

        double max = 0.0;
        for (Scenario scenario : scenarios)
            max = Math.max(max, scenario.shape(dateTime));
        return max;
    }
}
