package tech.illuin.wombat.core.module;

import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.module.asset.AssetProcessorFactory;

/**
 * Declares how assets of a given {@link AssetType} are sourced and evaluated.
 * <p>
 * Each feature is provided as a {@link AssetProcessorFactory}, invoked once per asset; an empty factory means the module
 * does not support that feature, in which case the defaults registered for the asset-type's family apply.
 */
public interface WombatModule
{
    WombatType type();
}
