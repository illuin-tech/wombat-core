package tech.illuin.wombat.core.module;

import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.evaluation.WombatEvaluationResolver;
import tech.illuin.wombat.core.source.WombatSource;

import java.util.Optional;

/**
 * Declares how assets of a given {@link AssetType} are sourced and evaluated.
 * <p>
 * Each feature is provided as a {@link ProcessorFactory}, invoked once per asset; an empty factory means the module
 * does not support that feature, in which case the defaults registered for the asset-type's family apply.
 */
public interface WombatModule
{
    AssetType type();

    Class<? extends Asset> assetClass();

    default void close() {}

    default Optional<ProcessorFactory<WombatSource>> source()
    {
        return Optional.empty();
    }

    default Optional<ProcessorFactory<WombatActivityResolver>> activityResolver()
    {
        return Optional.empty();
    }

    default Optional<ProcessorFactory<WombatEvaluationResolver>> impactResolver()
    {
        return Optional.empty();
    }

    default Optional<ProcessorFactory<WombatEvaluationResolver>> costResolver()
    {
        return Optional.empty();
    }
}
