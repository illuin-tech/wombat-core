package tech.illuin.wombat.core.evaluation;

public interface AssetEvaluation
{
    String environmentId();

    String assetId();

    EvaluationType type();

    enum EvaluationType
    {
        IMPACT,
        COST
    }
}
