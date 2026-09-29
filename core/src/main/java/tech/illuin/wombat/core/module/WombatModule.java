package tech.illuin.wombat.core.module;

import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.context.WombatContext;
import tech.illuin.wombat.core.evaluation.WombatEvaluationResolver;
import tech.illuin.wombat.core.source.WombatSource;

import java.util.Optional;

public interface WombatModule
{
    AssetType type();

    Class<? extends Asset> assetClass();

    default void close() {}

    default Optional<WombatSource> createSource(WombatContext context)
    {
        return Optional.empty();
    }

    default Optional<WombatActivityResolver> createActivityResolver()
    {
        return Optional.empty();
    }

    default Optional<WombatEvaluationResolver> createImpactResolver()
    {
        return Optional.empty();
    }

    default Optional<WombatEvaluationResolver> createCostResolver()
    {
        return Optional.empty();
    }
}
