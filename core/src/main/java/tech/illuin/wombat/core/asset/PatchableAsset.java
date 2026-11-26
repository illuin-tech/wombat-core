package tech.illuin.wombat.core.asset;

import tech.illuin.wombat.core.asset.profile.AssetProfile;

/**
 * An asset whose profile can be replaced by the one a {@link tech.illuin.wombat.core.evaluation.WombatProfileResolver}
 * resolves: the asset class of a module providing a profile-resolver must implement it.
 */
public interface PatchableAsset extends Asset
{
    /**
     * @return this asset with {@code profile} in place of its own
     * @throws IllegalArgumentException when this asset cannot hold {@code profile}
     */
    PatchableAsset withProfile(AssetProfile profile);
}
