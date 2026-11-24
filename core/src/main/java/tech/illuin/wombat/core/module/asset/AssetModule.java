package tech.illuin.wombat.core.module.asset;

import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.evaluation.WombatEvaluationResolver;
import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.core.source.WombatSource;

import java.util.Optional;

public interface AssetModule extends WombatModule
{
    AssetType type();

    Class<? extends Asset> assetClass();

    default void close() {}

    default Optional<AssetProcessorFactory<WombatSource>> source()
    {
        return Optional.empty();
    }

    default Optional<AssetProcessorFactory<WombatActivityResolver>> activityResolver()
    {
        return Optional.empty();
    }

    default Optional<AssetProcessorFactory<WombatEvaluationResolver>> impactResolver()
    {
        return Optional.empty();
    }

    default Optional<AssetProcessorFactory<WombatEvaluationResolver>> costResolver()
    {
        return Optional.empty();
    }
}
