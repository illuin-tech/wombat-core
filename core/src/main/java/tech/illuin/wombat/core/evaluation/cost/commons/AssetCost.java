package tech.illuin.wombat.core.evaluation.cost.commons;

import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.evaluation.AssetEvaluation;

public record AssetCost(
    AssetIdentity assetIdentity
) implements AssetEvaluation
{
    @Override
    public EvaluationType type()
    {
        return EvaluationType.COST;
    }
}
