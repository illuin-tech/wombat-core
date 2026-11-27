package tech.illuin.wombat.core.evaluation;

import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.profile.AssetProfile;
import tech.illuin.wombat.core.module.AssetProcessor;

/**
 * Resolves the profile an asset is evaluated with, for assets whose declared profile is only a request to infer it.
 * <p>
 * Only consulted for the assets it {@link #accept(Asset) accepts}: the others are evaluated with their declared profile.
 */
public interface WombatProfileResolver extends AssetProcessor
{
    AssetProfile resolve(Asset asset) throws WombatEvaluationException;
}
