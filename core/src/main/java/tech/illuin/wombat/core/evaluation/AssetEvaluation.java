package tech.illuin.wombat.core.evaluation;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.core.activity.commons.Timeboxed;
import tech.illuin.wombat.core.asset.AssetIdentity;

public interface AssetEvaluation extends Timeboxed
{
    AssetIdentity assetIdentity();

    /** Served, so that clients can tell an impact from a cost. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    EvaluationType type();

    enum EvaluationType
    {
        IMPACT,
        COST
    }
}
