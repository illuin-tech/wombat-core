package tech.illuin.wombat.core.evaluation.cost.commons;

import tech.illuin.wombat.core.evaluation.AssetEvaluation;

public record AssetCost(
    String environmentId,
    String assetId
) implements AssetEvaluation
{
    @Override
    public EvaluationType type()
    {
        return EvaluationType.COST;
    }
}
