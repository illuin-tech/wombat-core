package tech.illuin.wombat.module.conformance;

import tech.illuin.wombat.core.WombatCore;
import tech.illuin.wombat.core.activity.WombatActivityException;
import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.commons.AssetActivity;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.Environment;
import tech.illuin.wombat.core.asset.type.FamilyType;
import tech.illuin.wombat.core.context.WombatContext;
import tech.illuin.wombat.core.evaluation.AssetEvaluator;
import tech.illuin.wombat.core.evaluation.WombatEvaluationException;
import tech.illuin.wombat.core.evaluation.WombatEvaluationResolver;
import tech.illuin.wombat.core.evaluation.impact.commons.AssetImpact;
import tech.illuin.wombat.core.evaluation.impact.kubernetes.KubernetesMetricResolver;
import tech.illuin.wombat.core.evaluation.impact.llm.LLMMetricResolver;
import tech.illuin.wombat.core.module.MetricResolvers;
import tech.illuin.wombat.core.module.ProcessorFactory;
import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.core.module.WombatServices;
import tech.illuin.wombat.core.secret.SecretResolver;
import tech.illuin.wombat.module.conformance.canonical.CanonicalEngine;
import tech.illuin.wombat.module.conformance.canonical.CanonicalImpacts;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.mockito.Mockito.mock;

/**
 * A {@link WombatCore} with the module under test, and the engine under test as a module of its family, as the Boavizta
 * and Ecologits modules are.
 */
public final class ConformanceCore
{
    private final WombatModule module;
    private final CanonicalEngine engine;

    private static final String ENVIRONMENT = "conformance";
    private static final Duration HOUR = Duration.ofHours(1);

    public ConformanceCore(WombatModule module, CanonicalEngine engine)
    {
        this.module = Objects.requireNonNull(module, "module cannot be null");
        this.engine = Objects.requireNonNull(engine, "engine cannot be null");
    }

    public CanonicalEngine engine()
    {
        return this.engine;
    }

    /** Wombat's impacts of {@code asset} over {@code range}, one per slot of its resolution. */
    public List<AssetImpact> evaluate(Asset asset, TimeRange range) throws WombatEvaluationException
    {
        try (WombatCore core = this.core(asset); AssetEvaluator evaluator = core.createEvaluator())
        {
            return evaluator.evaluate(range, null, AssetImpact.class);
        }
    }

    /** Wombat's impacts of {@code asset} over {@code range}, summed over the slots in {@code double}. */
    public CanonicalImpacts total(Asset asset, TimeRange range) throws WombatEvaluationException
    {
        return this.evaluate(asset, range).stream()
            .map(impact -> CanonicalImpacts.of(impact.footprint()))
            .reduce(CanonicalImpacts::plus)
            .orElseThrow(() -> new IllegalStateException("no impact was evaluated for asset " + asset.identity().id()))
        ;
    }

    /** What the engine itself gives for {@code asset} over {@code range}, from the activity the module resolves hourly. */
    public CanonicalImpacts reference(Asset asset, TimeRange range) throws WombatActivityException
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
            new WombatServices(SecretResolver.standard(), new MetricResolvers(mock(KubernetesMetricResolver.class), mock(LLMMetricResolver.class)), List.of()),
            (identity, type, metrics) -> {},
            List.of(this.module, new EngineModule(this.engine))
        );
    }

    /** Stands for the methodology the engine reproduces, built on recorded responses rather than on a live API. */
    private record EngineModule(CanonicalEngine engine) implements WombatModule
    {
        @Override
        public FamilyType type()
        {
            return FamilyType.of("tech.illuin", "wombat-conformance", "engine", this.engine.family());
        }

        @Override
        public Optional<ProcessorFactory<WombatEvaluationResolver>> impactResolver()
        {
            return Optional.of((services, asset) -> this.engine.resolver());
        }
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
