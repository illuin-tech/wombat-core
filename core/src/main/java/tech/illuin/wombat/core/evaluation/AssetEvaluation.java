package tech.illuin.wombat.core.evaluation;

import tech.illuin.wombat.core.activity.commons.Timeboxed;
import tech.illuin.wombat.core.asset.AssetIdentity;

public interface AssetEvaluation extends Timeboxed
{
    AssetIdentity assetIdentity();

    EvaluationType type();

    enum EvaluationType
    {
        IMPACT,
        COST
    }
}
