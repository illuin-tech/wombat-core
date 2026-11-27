package tech.illuin.wombat.core.module;

import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.type.ModuleType;
import tech.illuin.wombat.core.evaluation.WombatEvaluationResolver;
import tech.illuin.wombat.core.evaluation.WombatProfileResolver;
import tech.illuin.wombat.core.source.WombatSource;

import java.util.Optional;

/**
 * Declares how the assets a module serves are sourced and evaluated.
 * <p>
 * A module serves the assets of the {@code AssetType} it defines, or every asset of a family when its type is a
 * {@code FamilyType}, e.g. an impact methodology (see {@link ModuleType}). Each feature is provided as a
 * {@link ProcessorFactory}, invoked once per asset; an empty factory means the module does not provide that feature.
 * <p>
 * For a given asset and feature, the module of the asset's type is asked first, then the family modules of its family
 * in the order they were registered: the first component to {@link AssetProcessor#accept(Asset) accept} the asset serves
 * it.
 */
public interface WombatModule
{
    ModuleType type();

    /**
     * @return the class the assets of the module's {@code AssetType} are read as; empty for a {@code FamilyType}, which
     *         defines no asset
     */
    default Optional<Class<? extends Asset>> assetClass()
    {
        return Optional.empty();
    }

    /**
     * @return the class of the module's own {@link ModuleSettings}, empty when it has none
     */
    default Optional<Class<? extends ModuleSettings>> settingsClass()
    {
        return Optional.empty();
    }

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

    default Optional<ProcessorFactory<WombatProfileResolver>> profileResolver()
    {
        return Optional.empty();
    }
}
