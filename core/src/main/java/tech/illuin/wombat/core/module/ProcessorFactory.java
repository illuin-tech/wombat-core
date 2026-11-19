package tech.illuin.wombat.core.module;

import tech.illuin.wombat.core.asset.Asset;

@FunctionalInterface
public interface ProcessorFactory<P extends AssetProcessor>
{
    /**
     * Builds the processor serving the asset identified by {@code asset.identity()}.
     * <p>
     * Called lazily, once per identity: the processor then receives the current asset on every call, so only what
     * cannot be read at runtime should be derived from {@code asset} here.
     * <p>
     * If the returned processor implements {@link AutoCloseable}, it is closed when its asset leaves the catalog, is
     * refreshed, or when its owner is closed. A processor shared across assets must therefore not implement
     * {@link AutoCloseable}; release its resources from {@link WombatModule#close()} instead.
     */
    P create(WombatServices services, Asset asset);
}
