package tech.illuin.wombat.module.kubernetes_static.scenario.evaluator;

import tech.illuin.wombat.module.kubernetes_static.scenario.Scenario;

import java.time.ZonedDateTime;
import java.util.List;

public interface ScenarioEvaluator
{
    double evaluate(ZonedDateTime dateTime, List<Scenario> scenarios);
}
