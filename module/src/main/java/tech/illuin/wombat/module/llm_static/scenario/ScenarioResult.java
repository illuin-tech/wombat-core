package tech.illuin.wombat.module.llm_static.scenario;

public record ScenarioResult(
    double requestPerYear,
    int outputTokenPerRequest
) {
    public static final ScenarioResult EMPTY = new ScenarioResult(0.0, 0);
}
