package tech.illuin.wombat.core.evaluation;

import java.util.List;

/**
 * The evaluations of the assets, and the assets that could not be evaluated.
 */
public record EvaluationReport(
    List<AssetEvaluation> evaluations,
    List<AssetEvaluationFailure> failures
)
{
    public EvaluationReport
    {
        evaluations = evaluations != null ? List.copyOf(evaluations) : List.of();
        failures = failures != null ? List.copyOf(failures) : List.of();
    }

    public <E extends AssetEvaluation> List<E> evaluations(Class<E> evalType)
    {
        return this.evaluations.stream()
            .filter(evalType::isInstance)
            .map(evalType::cast)
            .toList();
    }
}
