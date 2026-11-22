package tech.illuin.wombat.module.llm_static.scenario.evaluator;

import tech.illuin.wombat.module.llm_static.scenario.Scenario;

import java.time.ZonedDateTime;
import java.util.List;

public interface ScenarioEvaluator
{
    /** The model's traffic shape at the given instant, relative: the model's annual volume sets the scale. */
    double evaluate(ZonedDateTime dateTime, List<Scenario> scenarios);
}
