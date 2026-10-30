package tech.illuin.wombat.core.evaluation;

import tech.illuin.wombat.core.asset.AssetIdentity;

public interface AssetEvaluation
{
    AssetIdentity assetIdentity();

    EvaluationType type();

    enum EvaluationType
    {
        IMPACT,
        COST
    }
}
