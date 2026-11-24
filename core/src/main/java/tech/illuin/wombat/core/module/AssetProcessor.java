package tech.illuin.wombat.core.module;

import tech.illuin.wombat.core.asset.Asset;

public interface AssetProcessor
{
    /**
     * Tells whether this processor serves {@code asset}, as it currently is.
     * <p>
     * Asked of each candidate in turn when a processor is chosen for an asset, once per asset identity: the choice is
     * then kept until the asset is refreshed or leaves the context, and the processor is not asked again on each call.
     * It should therefore depend on what identifies the kind of asset, e.g. its class or the kind of its profile, rather
     * than on settings an edit may change.
     */
    default boolean accept(Asset asset)
    {
        return true;
    }
}
