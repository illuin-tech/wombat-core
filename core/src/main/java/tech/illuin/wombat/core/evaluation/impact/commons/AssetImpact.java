package tech.illuin.wombat.core.evaluation.impact.commons;

import tech.illuin.wombat.core.evaluation.AssetEvaluation;

import java.util.List;

public record AssetImpact(
    String environmentId,
    String assetId,
    Footprint footprint,
    List<ServiceImpact> serviceImpacts,
    ImpactProvider provider
) implements AssetEvaluation
{
    @Override
    public EvaluationType type()
    {
        return EvaluationType.IMPACT;
    }
}
