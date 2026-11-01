package tech.illuin.wombat.module.kubernetes_static.scenario.evaluator;

import tech.illuin.wombat.module.kubernetes_static.scenario.Scenario;

import java.time.ZonedDateTime;
import java.util.List;

public class CompositeScenarioEvaluator implements ScenarioEvaluator
{
    @Override
    public double evaluate(ZonedDateTime dateTime, List<Scenario> scenarios)
    {
        if (scenarios == null || scenarios.isEmpty() || dateTime == null)
            return 0.0;

        double maxLoad = 0.0;
        for (Scenario scenario : scenarios)
            if (scenario.isApplicable(dateTime))
                maxLoad = Math.max(maxLoad, scenario.evaluateLoad(dateTime));

        return maxLoad;
    }
}
