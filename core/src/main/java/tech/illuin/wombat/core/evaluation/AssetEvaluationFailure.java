package tech.illuin.wombat.core.evaluation;

import tech.illuin.wombat.core.asset.AssetIdentity;

/**
 * An asset that could not be evaluated, e.g. whose profile could not be inferred.
 *
 * @param reason why, as told by the failure
 */
public record AssetEvaluationFailure(
    AssetIdentity assetIdentity,
    String reason
) {}
