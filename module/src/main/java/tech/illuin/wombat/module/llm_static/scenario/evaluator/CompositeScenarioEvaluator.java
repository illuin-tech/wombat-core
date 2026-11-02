package tech.illuin.wombat.module.llm_static.scenario.evaluator;

import tech.illuin.wombat.module.llm_static.scenario.Scenario;
import tech.illuin.wombat.module.llm_static.scenario.ScenarioResult;

import java.time.ZonedDateTime;
import java.util.List;

public class CompositeScenarioEvaluator implements ScenarioEvaluator
{
    @Override
    public ScenarioResult evaluate(ZonedDateTime dateTime, List<Scenario> scenarios)
    {
        if (scenarios == null || scenarios.isEmpty() || dateTime == null)
            return ScenarioResult.EMPTY;

        Scenario bestScenario = null;
        double maxRequest = -1.0;

        for (Scenario scenario : scenarios)
        {
            if (scenario.isApplicable(dateTime))
            {
                double req = scenario.evaluateRequestPerYear(dateTime);
                if (req > maxRequest)
                {
                    maxRequest = req;
                    bestScenario = scenario;
                }
            }
        }

        if (bestScenario == null)
            return ScenarioResult.EMPTY;

        return new ScenarioResult(maxRequest, bestScenario.outputTokenPerRequest());
    }
}
