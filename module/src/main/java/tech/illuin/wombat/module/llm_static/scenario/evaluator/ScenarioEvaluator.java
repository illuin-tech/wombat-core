package tech.illuin.wombat.module.llm_static.scenario.evaluator;

import tech.illuin.wombat.module.llm_static.scenario.Scenario;
import tech.illuin.wombat.module.llm_static.scenario.ScenarioResult;

import java.time.ZonedDateTime;
import java.util.List;

public interface ScenarioEvaluator
{
    ScenarioResult evaluate(ZonedDateTime dateTime, List<Scenario> scenarios);
}
