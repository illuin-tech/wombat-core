package tech.illuin.wombat.core.evaluation;

import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.module.AssetProcessor;

public interface WombatEvaluationResolver extends AssetProcessor
{
    AssetEvaluation resolve(Asset asset, ActivityData activity) throws WombatEvaluationException;
}
