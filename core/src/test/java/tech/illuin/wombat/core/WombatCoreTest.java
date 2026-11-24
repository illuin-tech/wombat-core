package tech.illuin.wombat.core;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.commons.AssetActivity;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.Environment;
import tech.illuin.wombat.core.asset.type.FamilyType;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.asset.profile.AssetProfile;
import tech.illuin.wombat.core.context.ResolvedContext;
import tech.illuin.wombat.core.context.WombatContextProvider;
import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.commons.AssetFilter;
import tech.illuin.wombat.core.activity.llm.LLMActivityData;
import tech.illuin.wombat.core.evaluation.AssetEvaluation;
import tech.illuin.wombat.core.evaluation.WombatEvaluationResolver;
import tech.illuin.wombat.core.evaluation.AssetEvaluator;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.evaluation.impact.kubernetes.KubernetesMetricResolver;
import tech.illuin.wombat.core.evaluation.impact.llm.LLMMetricResolver;
import tech.illuin.wombat.core.module.MetricResolvers;
import tech.illuin.wombat.core.module.ProcessorFactory;
import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.core.module.WombatServices;
import tech.illuin.wombat.core.secret.SecretResolver;
import tech.illuin.wombat.core.source.Monitorable;
import tech.illuin.wombat.core.source.Period;
import tech.illuin.wombat.core.source.WombatSource;
import tech.illuin.wombat.core.source.data.MetricData;
import tech.illuin.wombat.core.source.AssetMonitor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static tech.illuin.wombat.core.activity.commons.Country.FRANCE;

class WombatCoreTest
{
    private static final AssetType TYPE_PROMETHEUS = AssetType.of("tech.illuin", "wombat-module", "llm-prometheus", ActivityRegime.MEASURED, ServiceFamily.LLM);
    private static final AssetType TYPE_STATIC = AssetType.of("tech.illuin", "wombat-module", "llm-static", ActivityRegime.MODELED, ServiceFamily.LLM);
    private static final FamilyType TYPE_FAMILY = FamilyType.of("tech.illuin", "wombat-module", "llm-family", ServiceFamily.LLM);
    private static final FamilyType TYPE_FAMILY_SECOND = FamilyType.of("tech.illuin", "wombat-module", "llm-family-second", ServiceFamily.LLM);
    private static final FamilyType TYPE_FAMILY_OTHER = FamilyType.of("tech.illuin", "wombat-module", "kubernetes-family", ServiceFamily.KUBERNETES_CONTAINER);

    @Test
    void createMonitor_registersASourceForEverySourceableAsset()
    {
        SourcingModule module = new SourcingModule();
        try (WombatCore core = core(context(sourceable("a1"), sourceable("a2")), module) ; AssetMonitor monitor = core.createMonitor()) {
            monitor.trigger();
        }
        // The monitor dispatches one task per asset, so what it guarantees is that each was sampled once — not the order in which the executor got to them.
        assertEquals(2, module.source.sampled.size());
        assertEquals(Set.of("a1", "a2"), Set.copyOf(module.source.sampled));
    }

    @Test
    void createMonitor_skipsModulesThatOnlyResolveImpacts()
    {
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
    void createMonitor_sourcesAssetsAddedAfterItsCreation()
    {
        SourcingModule module = new SourcingModule();
        List<Asset> assets = new CopyOnWriteArrayList<>();
        WombatContextProvider context = () -> new ResolvedContext(List.of(new Environment("env", List.copyOf(assets))));

        // Neither the asset nor its type are known when the monitor is created.
        try (WombatCore core = core(context, module); AssetMonitor monitor = core.createMonitor()) {
            assets.add(sourceable("a1"));
            monitor.trigger();
        }

        assertEquals(List.of("a1"), module.source.sampled);
    }

    @Test
    void createCalculator_resolvesOnlyAssetsServedByAnImpactResolver() throws Exception
    {
        // Both assets share the LLM family, so this family module serves the two of them: the activity-resolver every
        // asset needs to be evaluated at all, and impact/cost resolvers that accept none, which leaves the modeled
        // asset's own module as the only thing able to resolve an impact.
        FamilyModule family = new FamilyModule(TYPE_FAMILY, ACTIVITY, SERVES_NOTHING);

        // The sourceable asset's module provides no impact-resolver, so only the modeled one is calculated.
        try (WombatCore core = core(context(sourceable("a1"), modeled("static-1")), new SourcingModule(), new ResolvingModule(), family) ; AssetEvaluator evaluator = core.createEvaluator())
        {
            List<AssetEvaluation> impacts = evaluator.evaluate(RANGE, AssetFilter.none());

            assertEquals(1, impacts.size());
            assertEquals("static-1", impacts.getFirst().assetIdentity().id());
        }
    }

    @Test
    void createCalculator_servesAnAssetTypeFromTheModulesOfItsFamily() throws Exception
    {
        FamilyModule family = new FamilyModule(TYPE_FAMILY, ACTIVITY, impactResolver("family"));

        try (WombatCore core = core(context(sourceable("a1")), new SourcingModule(), family) ; AssetEvaluator evaluator = core.createEvaluator())
        {
            assertEquals(List.of("family"), providers(evaluator.evaluate(RANGE, AssetFilter.none())));
        }
    }

    @Test
    void createCalculator_prefersTheComponentOfTheAssetTypesOwnModule() throws Exception
    {
        FamilyModule family = new FamilyModule(TYPE_FAMILY, ACTIVITY, impactResolver("family"));

        try (WombatCore core = core(context(modeled("static-1")), family, new ResolvingModule()) ; AssetEvaluator evaluator = core.createEvaluator())
        {
            assertEquals(List.of("own"), providers(evaluator.evaluate(RANGE, AssetFilter.none())));
        }
    }

    @Test
    void createCalculator_triesTheModulesOfAFamilyInRegistrationOrder() throws Exception
    {
        // The first family module rejects the asset: the second one, then the one of another family, are next in line.
        FamilyModule other = new FamilyModule(TYPE_FAMILY_OTHER, ACTIVITY, impactResolver("other"));
        FamilyModule first = new FamilyModule(TYPE_FAMILY, ACTIVITY, SERVES_NOTHING);
        FamilyModule second = new FamilyModule(TYPE_FAMILY_SECOND, ACTIVITY, impactResolver("second"));

        try (WombatCore core = core(context(sourceable("a1")), new SourcingModule(), other, first, second) ; AssetEvaluator evaluator = core.createEvaluator())
        {
            assertEquals(List.of("second"), providers(evaluator.evaluate(RANGE, AssetFilter.none())));
        }
    }

    @Test
    void createMonitor_sourcesAssetsFromTheModulesOfTheirFamily()
    {
        RecordingSource source = new RecordingSource();
        FamilyModule family = new FamilyModule(TYPE_FAMILY, ACTIVITY, SERVES_NOTHING)
        {
            @Override
            public Optional<ProcessorFactory<WombatSource>> source()
            {
                return Optional.of((services, asset) -> source);
            }
        };

        // The asset-type's module sources nothing: the family module does.
        try (WombatCore core = core(context(sourceable("a1")), new ResolvingModule(TYPE_PROMETHEUS, SourceableAsset.class), family) ; AssetMonitor monitor = core.createMonitor()) {
            monitor.trigger();
        }

        assertEquals(List.of("a1"), source.sampled);
    }

    @Test
    void createCalculator_skipsAssetTypesWithoutAnyResolver() throws Exception
    {
        // No defaults are registered, and the module provides no activity-resolver: there is nothing to evaluate with.
        try (WombatCore core = core(context(modeled("static-1")), new ResolvingModule()) ; AssetEvaluator evaluator = assertDoesNotThrow(() -> core.createEvaluator()))
        {
            assertEquals(List.of(), evaluator.evaluate(RANGE, AssetFilter.none()));
        }
    }

    @Test
    void register_rejectsASecondModuleForTheSameAssetType()
    {
        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> core(context(sourceable("a1")), new SourcingModule(), new SourcingModule())
        );

        assertTrue(error.getMessage().contains(TYPE_PROMETHEUS.name()), error.getMessage());
    }

    @Test
    void register_rejectsAFamilyModuleNamedAfterAnAssetType()
    {
        FamilyType clash = FamilyType.of(TYPE_PROMETHEUS.namespace(), TYPE_PROMETHEUS.localName(), ServiceFamily.LLM);

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> core(context(sourceable("a1")), new SourcingModule(), new FamilyModule(clash, ACTIVITY, SERVES_NOTHING))
        );

        assertTrue(error.getMessage().contains(TYPE_PROMETHEUS.name()), error.getMessage());
    }

    @Test
    void register_rejectsAnAssetTypeWithoutAssetClass()
    {
        assertThrows(IllegalArgumentException.class, () -> core(context(), new ResolvingModule(TYPE_STATIC, null)));
    }

    @Test
    void register_rejectsAFamilyModuleWithAnAssetClass()
    {
        FamilyModule family = new FamilyModule(TYPE_FAMILY, ACTIVITY, SERVES_NOTHING)
        {
            @Override
            public Optional<Class<? extends Asset>> assetClass()
            {
                return Optional.of(ModeledAsset.class);
            }
        };

        assertThrows(IllegalArgumentException.class, () -> core(context(), family));
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

    private static final WombatServices SERVICES = new WombatServices(
        SecretResolver.standard(),
        new MetricResolvers(mock(KubernetesMetricResolver.class), mock(LLMMetricResolver.class)),
        List.of()
    );

    private static WombatCore core(WombatContextProvider context, WombatModule... modules)
    {
        return new WombatCore(context, SERVICES, (asset, type, metrics) -> {}, List.of(modules));
    }

    /** Names the module whose impact-resolver produced each evaluation. */
    private static List<String> providers(List<AssetEvaluation> evaluations)
    {
        return evaluations.stream()
            .map(LabeledImpact.class::cast)
            .map(LabeledImpact::module)
            .toList();
    }

    private static WombatEvaluationResolver impactResolver(String module)
    {
        return (asset, data) -> new LabeledImpact(module, asset.identity(), data.range());
    }

    /**
     * Stands in for the activity a family module resolves for every asset of its family, e.g. out of the metric store.
     */
    private static final WombatActivityResolver ACTIVITY = (asset, range, filter) -> Optional.of(new AssetActivity(
        asset.identity(),
        List.of(new LLMActivityData(
            ActivityRegime.MODELED,
            asset.identity().id(),
            range,
            LLMProvider.mistralai,
            "test",
            FRANCE,
            0L,
            0
        ))
    ));

    /**
     * Stands in for a default that serves nothing at this level: the evaluator skips every asset it is offered.
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

    private record SourceableAsset(String assetId) implements Asset, Monitorable
    {
        @Override
        public AssetIdentity identity()
        {
            return AssetIdentity.of(this.assetId, "env", this.type());
        }

        @Override
        public AssetType type()
        {
            return TYPE_PROMETHEUS;
        }

        @Override
        public AssetProfile profile()
        {
            return PROFILE;
        }

        @Override
        public Period period()
        {
            return Period.ONE_SECOND;
        }
    }

    /** Deliberately NOT a MonitoredAsset: its activity is modeled, so there is nothing to sample. */
    private record ModeledAsset(String assetId) implements Asset
    {
        @Override
        public AssetIdentity identity()
        {
            return AssetIdentity.of(this.assetId, "env", this.type());
        }

        @Override
        public AssetType type()
        {
            return TYPE_STATIC;
        }

        @Override
        public AssetProfile profile()
        {
            return PROFILE;
        }
    }

    private static final AssetProfile PROFILE = new AssetProfile() {};

    private static final class SourcingModule implements WombatModule
    {
        private final RecordingSource source = new RecordingSource();
        private boolean closed;

        @Override
        public AssetType type()
        {
            return TYPE_PROMETHEUS;
        }

        @Override
        public Optional<Class<? extends Asset>> assetClass()
        {
            return Optional.of(SourceableAsset.class);
        }

        @Override
        public Optional<ProcessorFactory<WombatSource>> source()
        {
            return Optional.of((services, asset) -> this.source);
        }

        @Override
        public void close()
        {
            this.closed = true;
        }
    }

    private static final class ResolvingModule implements WombatModule
    {
        private final AssetType type;
        private final Class<? extends Asset> assetClass;
        private boolean closed;

        ResolvingModule()
        {
            this(TYPE_STATIC, ModeledAsset.class);
        }

        ResolvingModule(AssetType type, Class<? extends Asset> assetClass)
        {
            this.type = type;
            this.assetClass = assetClass;
        }

        @Override
        public AssetType type()
        {
            return this.type;
        }

        @Override
        public Optional<Class<? extends Asset>> assetClass()
        {
            return Optional.ofNullable(this.assetClass);
        }

        @Override
        public Optional<ProcessorFactory<WombatEvaluationResolver>> impactResolver()
        {
            return Optional.of((services, asset) -> WombatCoreTest.impactResolver("own"));
        }

        @Override
        public void close()
        {
            this.closed = true;
        }
    }

    /** Serves every asset of its family, as an impact methodology or the metric store's activity would. */
    private static class FamilyModule implements WombatModule
    {
        private final FamilyType type;
        private final WombatActivityResolver activity;
        private final WombatEvaluationResolver impact;

        FamilyModule(FamilyType type, WombatActivityResolver activity, WombatEvaluationResolver impact)
        {
            this.type = type;
            this.activity = activity;
            this.impact = impact;
        }

        @Override
        public FamilyType type()
        {
            return this.type;
        }

        @Override
        public Optional<ProcessorFactory<WombatActivityResolver>> activityResolver()
        {
            return Optional.of((services, asset) -> this.activity);
        }

        @Override
        public Optional<ProcessorFactory<WombatEvaluationResolver>> impactResolver()
        {
            return Optional.of((services, asset) -> this.impact);
        }

        @Override
        public Optional<ProcessorFactory<WombatEvaluationResolver>> costResolver()
        {
            return Optional.of((services, asset) -> SERVES_NOTHING);
        }
    }

    private record LabeledImpact(String module, AssetIdentity assetIdentity, TimeRange range) implements AssetEvaluation
    {
        @Override
        public EvaluationType type()
        {
            return EvaluationType.IMPACT;
        }
    }

    private static final class RecordingSource implements WombatSource
    {
        /** Written from the monitor's executor threads, read by the test once the monitor is closed. */
        private final List<String> sampled = Collections.synchronizedList(new ArrayList<>());

        @Override
        public List<MetricData> source(Instant heartbeat, Instant lastActivation, Asset asset)
        {
            this.sampled.add(asset.identity().id());
            return emptyList();
        }
    }
}
