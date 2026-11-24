package tech.illuin.wombat.core.module;

import tech.illuin.wombat.core.asset.Asset;

public interface AssetProcessor
{
    /**
     * Tells whether this processor serves {@code asset}.
     * <p>
     * Asked once per asset identity, when a processor is chosen for it: the choice is kept until the asset is
     * refreshed or leaves the context. It should therefore depend on the kind of asset, e.g. its class or the kind of
     * its profile, rather than on settings an edit may change.
     */
    default boolean accept(Asset asset)
    {
        return true;
    }
}
