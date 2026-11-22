package tech.illuin.wombat.module.conformance.canonical;

import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.evaluation.WombatEvaluationResolver;

import java.util.List;

/**
 * A methodology engine called the way its own documentation does: the reference that Wombat's evaluation through this
 * engine is held to. Boavizta and Ecologits are the first ones, each engine to come brings its own implementation.
 * <p>
 * It reads the activity Wombat resolves at hourly resolution, the finest every module provides, so that one engine
 * serves every module of its family. Whether that activity is right is for the activity resolvers' tests to check.
 * <p>
 * Its requests are written here rather than through Wombat's request models, so that a mapping mistake there cannot be
 * shared by the reference.
 */
public interface CanonicalEngine
{
    /** The family whose assets this engine evaluates. */
    ServiceFamily family();

    /** Wombat's evaluation through this engine, the one held to this reference. */
    WombatEvaluationResolver resolver();

    /** The engine's impacts for {@code asset} over its hourly {@code activity}, the whole range in one go where the engine allows it. */
    CanonicalImpacts evaluate(Asset asset, List<ActivityData> activity);

    /** How far Wombat's figures may stray from this engine's, its own rounding included. */
    Tolerance tolerance();
}
