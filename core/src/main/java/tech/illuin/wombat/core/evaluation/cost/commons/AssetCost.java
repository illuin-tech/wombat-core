package tech.illuin.wombat.core.evaluation.cost.commons;

import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.evaluation.AssetEvaluation;

import java.util.List;

/**
 * What an asset costs over a range, the counterpart of an {@code AssetImpact}.
 *
 * @param cost         the asset's cost; {@code null} when none was computed, e.g. by a resolver that prices nothing yet
 * @param serviceCosts what each of its services costs; empty when none was computed
 */
public record AssetCost(
    AssetIdentity assetIdentity,
    Money cost,
    List<ServiceCost> serviceCosts,
    TimeRange range
) implements AssetEvaluation
{
    public AssetCost
    {
        serviceCosts = serviceCosts != null ? serviceCosts : List.of();
    }

    @Override
    public EvaluationType type()
    {
        return EvaluationType.COST;
    }
}
