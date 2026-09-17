package tech.illuin.wombat.core;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.ActivityRegime;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetType;
import tech.illuin.wombat.core.asset.Environment;
import tech.illuin.wombat.core.asset.ServiceFamily;
import tech.illuin.wombat.core.asset.profile.Profile;
import tech.illuin.wombat.core.context.ResolvedContext;
import tech.illuin.wombat.core.context.WombatContext;
import tech.illuin.wombat.core.context.WombatContextProvider;
import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.commons.AssetFilter;
import tech.illuin.wombat.core.activity.llm.LLMActivityData;
import tech.illuin.wombat.core.evaluation.AssetEvaluation;
import tech.illuin.wombat.core.evaluation.WombatEvaluationResolver;
import tech.illuin.wombat.core.evaluation.AssetEvaluator;
import tech.illuin.wombat.core.evaluation.impact.commons.AssetImpact;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.core.source.Monitorable;
import tech.illuin.wombat.core.source.WombatSource;
import tech.illuin.wombat.core.source.data.MetricData;
import tech.illuin.wombat.core.source.AssetMonitor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WombatCoreTest
{

    @Test
    void createMonitor_registersASourceForEverySourceableAsset()
    {
        SourcingModule module = new SourcingModule();
        try (WombatCore core = core(context(sourceable("a1"), sourceable("a2")), module) ; AssetMonitor monitor = core.createMonitor()) {
            monitor.trigger();
        }
        // The monitor dispatches one task per asset, so what it guarantees is that each was sampled once — not the
        // order in which the executor got to them.
        assertEquals(2, module.source.sampled.size());
        assertEquals(Set.of("a1", "a2"), Set.copyOf(module.source.sampled));
    }

    @Test
    void createMonitor_skipsModulesThatOnlyResolveImpacts()
    {
        // Regression: the MonitoredAsset guard used to run before the source-provider check, so a modeled asset —
        // an Asset that is deliberately not monitorable, served by an impact-only module — threw here instead of
        // being skipped, which aborted application startup.
        try (WombatCore core = core(context(modeled("static-1")), new ResolvingModule()) ; AssetMonitor monitor = assertDoesNotThrow(() -> core.createMonitor())) {
            assertDoesNotThrow(monitor::trigger);
        }
    }

    @Test
    void createMonitor_skipsAssetsWhoseTypeHasNoModule()
    {
        SourcingModule module = new SourcingModule();
        // The modeled asset's type has no module registered at all.
        try (WombatCore core = core(context(sourceable("a1"), modeled("static-1")), module); AssetMonitor monitor = core.createMonitor()) {
            monitor.trigger();
        }

        assertEquals(List.of("a1"), module.source.sampled);
    }

    @Test
    void createCalculator_resolvesOnlyAssetsServedByAnImpactResolver() throws Exception
    {
        ResolvingModule resolving = new ResolvingModule();
        // Both assets share the LLM family, so these defaults serve the two of them: the activity-resolver every
        // asset needs to be evaluated at all, and impact/cost defaults that accept none — which leaves the modeled
        // asset's own module as the only thing able to resolve an impact.
        Consumer<WombatCore.CoreDefaults> defaults = registry -> registry.register(
            ServiceFamily.LLM,
            (asset, range, filter) -> new LLMActivityData(ActivityRegime.MODELED, Set.of(asset.id()), range, 0L, 0),
            SERVES_NOTHING,
            SERVES_NOTHING
        );

        // The sourceable asset's module provides no impact-resolver, so only the modeled one is calculated.
        try (WombatCore core = core(context(sourceable("a1"), modeled("static-1")), defaults, new SourcingModule(), resolving) ; AssetEvaluator evaluator = core.createEvaluator())
        {
            List<AssetEvaluation> impacts = evaluator.evaluate(RANGE, AssetFilter.none());

            assertEquals(1, impacts.size());
            assertEquals("static-1", impacts.getFirst().assetId());
        }
    }

    @Test
    void register_rejectsASecondModuleForTheSameAssetType()
    {
        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> core(context(sourceable("a1")), new SourcingModule(), new SourcingModule())
        );

        assertTrue(error.getMessage().contains(AssetType.LLM_PROMETHEUS.name()), error.getMessage());
    }

    @Test
    void close_closesEveryRegisteredModule()
    {
        SourcingModule sourcing = new SourcingModule();
        ResolvingModule resolving = new ResolvingModule();

        core(context(sourceable("a1")), sourcing, resolving).close();

        assertTrue(sourcing.closed);
        assertTrue(resolving.closed);
    }

    private static final TimeRange RANGE = new TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1_000));

    private static WombatCore core(WombatContextProvider context, WombatModule... modules)
    {
        return core(context, defaults -> {}, modules);
    }

    private static WombatCore core(WombatContextProvider context, Consumer<WombatCore.CoreDefaults> defaults, WombatModule... modules)
    {
        return new WombatCore(context, metrics -> {}, List.of(modules), defaults);
    }

    /**
     * Stands in for "no default at this level": registration succeeds, so nothing fails at wiring time, and the
     * evaluator then skips every asset it is offered.
     */
    private static final WombatEvaluationResolver SERVES_NOTHING = new WombatEvaluationResolver()
    {
        @Override
        public boolean accept(Asset asset)
        {
            return false;
        }

        @Override
        public AssetEvaluation resolve(Asset asset, ActivityData activity)
        {
            throw new IllegalStateException("A resolver accepting no asset should never be asked to resolve one");
        }
    };

    private static WombatContextProvider context(Asset... assets)
    {
        return () -> new ResolvedContext(List.of(new Environment("env", List.of(assets))));
    }

    private static Asset sourceable(String id)
    {
        return new SourceableAsset(id);
    }

    private static Asset modeled(String id)
    {
        return new ModeledAsset(id);
    }

    private record SourceableAsset(String id) implements Asset, Monitorable
    {
        @Override
        public String environmentId()
        {
            return "env";
        }

        @Override
        public String name()
        {
            return this.id;
        }

        @Override
        public AssetType type()
        {
            return AssetType.LLM_PROMETHEUS;
        }

        @Override
        public Profile profile()
        {
            return PROFILE;
        }

        @Override
        public int heartbeatSkip()
        {
            return 1;
        }
    }

    /** Deliberately NOT a MonitoredAsset: its activity is modeled, so there is nothing to sample. */
    private record ModeledAsset(String id) implements Asset
    {
        @Override
        public String environmentId()
        {
            return "env";
        }

        @Override
        public String name()
        {
            return this.id;
        }

        @Override
        public AssetType type()
        {
            return AssetType.LLM_STATIC;
        }

        @Override
        public Profile profile()
        {
            return PROFILE;
        }
    }

    private static final Profile PROFILE = new Profile()
    {
        @Override
        public String id()
        {
            return "profile";
        }

        @Override
        public ServiceFamily serviceFamily()
        {
            return ServiceFamily.LLM;
        }
    };

    private static final class SourcingModule implements WombatModule
    {
        private final RecordingSource source = new RecordingSource();
        private boolean closed;

        @Override
        public AssetType type()
        {
            return AssetType.LLM_PROMETHEUS;
        }

        @Override
        public Class<? extends Asset> assetClass()
        {
            return SourceableAsset.class;
        }

        @Override
        public Optional<WombatSource> createSource(WombatContext context)
        {
            return Optional.of(this.source);
        }

        @Override
        public void close()
        {
            this.closed = true;
        }
    }

    private static final class ResolvingModule implements WombatModule
    {
        private boolean closed;

        @Override
        public AssetType type()
        {
            return AssetType.LLM_STATIC;
        }

        @Override
        public Class<? extends Asset> assetClass()
        {
            return ModeledAsset.class;
        }

        @Override
        public Optional<WombatEvaluationResolver> createImpactResolver(WombatContext context)
        {
            return Optional.of((resolved, data) -> new AssetImpact(
                resolved.environmentId(),
                resolved.id(),
                null,
                emptyList(),
                null
            ));
        }

        @Override
        public void close()
        {
            this.closed = true;
        }
    }

    private static final class RecordingSource implements WombatSource
    {
        /** Written from the monitor's executor threads, read by the test once the monitor is closed. */
        private final List<String> sampled = Collections.synchronizedList(new ArrayList<>());

        @Override
        public List<MetricData> source(Instant heartbeat, Asset asset)
        {
            this.sampled.add(asset.id());
            return emptyList();
        }
    }
}
