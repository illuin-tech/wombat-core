package tech.illuin.wombat.module.conformance;

import tech.illuin.wombat.core.WombatCore;
import tech.illuin.wombat.core.activity.WombatActivityException;
import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.commons.AssetActivity;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.Environment;
import tech.illuin.wombat.core.context.WombatContext;
import tech.illuin.wombat.core.evaluation.AssetEvaluator;
import tech.illuin.wombat.core.evaluation.WombatEvaluationException;
import tech.illuin.wombat.core.evaluation.impact.commons.AssetImpact;
import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.module.conformance.canonical.CanonicalEngine;
import tech.illuin.wombat.module.conformance.canonical.CanonicalImpacts;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * A {@link WombatCore} with the module under test, and the engine under test as its family's default impact component,
 * as the README's custom core registers them.
 */
final class ConformanceCore
{
    private final WombatModule module;
    private final CanonicalEngine engine;

    private static final String ENVIRONMENT = "conformance";
    private static final Duration HOUR = Duration.ofHours(1);

    ConformanceCore(WombatModule module, CanonicalEngine engine)
    {
        this.module = Objects.requireNonNull(module, "module cannot be null");
        this.engine = Objects.requireNonNull(engine, "engine cannot be null");
    }

    CanonicalEngine engine()
    {
        return this.engine;
    }

    /** Wombat's impacts of {@code asset} over {@code range}, one per slot of its resolution. */
    List<AssetImpact> evaluate(Asset asset, TimeRange range) throws WombatEvaluationException
    {
        try (WombatCore core = this.core(asset); AssetEvaluator evaluator = core.createEvaluator())
        {
            return evaluator.evaluate(range, null, AssetImpact.class);
        }
    }

    /** Wombat's impacts of {@code asset} over {@code range}, summed over the slots in {@code double}. */
    CanonicalImpacts total(Asset asset, TimeRange range) throws WombatEvaluationException
    {
        return this.evaluate(asset, range).stream()
            .map(impact -> CanonicalImpacts.of(impact.footprint()))
            .reduce(CanonicalImpacts::plus)
            .orElseThrow(() -> new IllegalStateException("no impact was evaluated for asset " + asset.identity().id()))
        ;
    }

    /** What the engine itself gives for {@code asset} over {@code range}, from the activity the module resolves hourly. */
    CanonicalImpacts reference(Asset asset, TimeRange range) throws WombatActivityException
    {
        try (WombatCore core = this.core(asset); AssetEvaluator evaluator = core.createEvaluator())
        {
            List<ActivityData> activity = evaluator.computeActivity(asset, new TimeRange(range.start(), range.end(), HOUR), null)
                .map(AssetActivity::activity)
                .orElseThrow(() -> new IllegalStateException("no activity was resolved for asset " + asset.identity().id()))
            ;
            return this.engine.evaluate(asset, activity);
        }
    }

    private WombatCore core(Asset asset)
    {
        Context context = new Context(List.of(new Environment(ENVIRONMENT, List.of(asset))));
        return new WombatCore(
            () -> context,
            (identity, type, metrics) -> {},
            List.of(this.module),
            defaults -> defaults.register(this.engine.family(), null, (services, a) -> this.engine.resolver(), null)
        );
    }

    private record Context(List<Environment> environments) implements WombatContext
    {
        @Override
        public List<Asset> assets()
        {
            return this.environments.stream().flatMap(environment -> environment.assets().stream()).toList();
        }
    }
}
