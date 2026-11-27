package tech.illuin.wombat.core.evaluation;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.commons.AssetActivity;
import tech.illuin.wombat.core.activity.commons.AssetFilter;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.Environment;
import tech.illuin.wombat.core.asset.PatchableAsset;
import tech.illuin.wombat.core.asset.profile.AssetProfile;
import tech.illuin.wombat.core.asset.profile.InferredProfile;
import tech.illuin.wombat.core.asset.profile.ProfileOrigin;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.context.ResolvedContext;
import tech.illuin.wombat.core.context.WombatContextProvider;
import tech.illuin.wombat.core.evaluation.impact.commons.AssetImpact;
import tech.illuin.wombat.core.evaluation.impact.commons.Footprint;
import tech.illuin.wombat.core.evaluation.impact.commons.ImpactProvider;
import tech.illuin.wombat.core.evaluation.impact.kubernetes.KubernetesMetricResolver;
import tech.illuin.wombat.core.evaluation.impact.llm.LLMMetricResolver;
import tech.illuin.wombat.core.module.MetricResolvers;
import tech.illuin.wombat.core.module.ProcessorFactory;
import tech.illuin.wombat.core.module.WombatServices;
import tech.illuin.wombat.core.secret.SecretResolver;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class AssetEvaluatorTest
{
    private static final AssetType TYPE = AssetType.of("tech.illuin", "wombat-module", "test", ActivityRegime.MODELED, ServiceFamily.LLM);
    private static final WombatServices SERVICES = new WombatServices(
        SecretResolver.standard(),
        new MetricResolvers(mock(KubernetesMetricResolver.class), mock(LLMMetricResolver.class)),
        List.of()
    );
    private static final TimeRange RANGE = new TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1_000));

    @Test
    void evaluate_resolvesAssetsAddedAfterItsCreation() throws WombatEvaluationException
    {
        List<Asset> assets = new CopyOnWriteArrayList<>();
        List<RecordingResolver> created = new CopyOnWriteArrayList<>();

        try (AssetEvaluator evaluator = evaluator(assets, created))
        {
            evaluator.report(RANGE, AssetFilter.none());
            assets.add(new TestAsset("a1"));
            evaluator.report(RANGE, AssetFilter.none());
        }

        assertEquals(1, created.size());
        assertEquals(List.of("a1"), created.getFirst().resolved);
    }

    @Test
    void evaluate_closesTheResolversOfRemovedAssets() throws WombatEvaluationException
    {
        List<Asset> assets = new CopyOnWriteArrayList<>(List.of(new TestAsset("a1"), new TestAsset("a2")));
        List<RecordingResolver> created = new CopyOnWriteArrayList<>();

        try (AssetEvaluator evaluator = evaluator(assets, created))
        {
            evaluator.report(RANGE, AssetFilter.none());
            assets.removeIf(asset -> asset.identity().id().equals("a1"));
            evaluator.report(RANGE, AssetFilter.none());

            RecordingResolver a1 = created.stream().filter(r -> r.resolved.contains("a1")).findFirst().orElseThrow();
            RecordingResolver a2 = created.stream().filter(r -> r.resolved.contains("a2")).findFirst().orElseThrow();
            assertEquals(1, a1.closed.get());
            assertEquals(0, a2.closed.get());
        }
    }

    @Test
    void refresh_rebuildsTheResolversOfAnAsset() throws WombatEvaluationException
    {
        TestAsset asset = new TestAsset("a1");
        List<RecordingResolver> created = new CopyOnWriteArrayList<>();

        try (AssetEvaluator evaluator = evaluator(List.of(asset), created))
        {
            evaluator.report(RANGE, AssetFilter.none());
            evaluator.refresh(asset.identity());
            evaluator.report(RANGE, AssetFilter.none());

            assertEquals(2, created.size());
            assertEquals(1, created.getFirst().closed.get());
            assertEquals(0, created.getLast().closed.get());
        }
    }

    @Test
    void evaluate_runsOnTheConfiguredExecutor() throws WombatEvaluationException
    {
        List<String> threads = new CopyOnWriteArrayList<>();
        ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "evaluator-test"));

        try (AssetEvaluator evaluator = new AssetEvaluator(contextOf(List.of(new TestAsset("a1"))), SERVICES, executor))
        {
            evaluator.registerActivityResolver(TYPE, (services, asset) -> (resolved, range, filter) -> {
                threads.add(Thread.currentThread().getName());
                return Optional.empty();
            });
            evaluator.report(RANGE, AssetFilter.none());
        }

        assertEquals(List.of("evaluator-test"), threads);
    }

    @Test
    void computeImpact_evaluatesWithTheResolvedProfile() throws WombatEvaluationException
    {
        AssetProfile inferred = new AssetProfile() {};
        List<AssetProfile> evaluated = new CopyOnWriteArrayList<>();

        try (AssetEvaluator evaluator = new AssetEvaluator(contextOf(List.of()), SERVICES))
        {
            evaluator.registerImpactResolver(TYPE, recordingProfiles(evaluated));
            evaluator.registerCostResolver(TYPE, recordingProfiles(evaluated));
            evaluator.registerProfileResolver(TYPE, (services, asset) -> resolved -> inferred);

            evaluator.computeImpact(new TestAsset("a1"), mock(ActivityData.class));
            evaluator.computeCost(new TestAsset("a1"), mock(ActivityData.class));
        }

        assertEquals(List.of(inferred, inferred), evaluated);
    }

    @Test
    void computeImpact_keepsTheDeclaredProfileWhenNoProfileResolverAcceptsIt() throws WombatEvaluationException
    {
        List<AssetProfile> evaluated = new CopyOnWriteArrayList<>();
        TestAsset asset = new TestAsset("a1");

        try (AssetEvaluator evaluator = new AssetEvaluator(contextOf(List.of()), SERVICES))
        {
            evaluator.registerImpactResolver(TYPE, recordingProfiles(evaluated));
            evaluator.registerProfileResolver(TYPE, (services, declared) -> new WombatProfileResolver() {
                @Override
                public boolean accept(Asset candidate)
                {
                    return false;
                }

                @Override
                public AssetProfile resolve(Asset candidate)
                {
                    throw new AssertionError("A profile resolver rejecting an asset must not resolve it");
                }
            });

            evaluator.computeImpact(asset, mock(ActivityData.class));
        }

        assertEquals(Collections.singletonList(asset.profile()), evaluated);
    }

    @Test
    void computeImpact_failsOnlyThisAssetWhenItsProfileResolverCannotBeBuilt()
    {
        try (AssetEvaluator evaluator = new AssetEvaluator(contextOf(List.of()), SERVICES))
        {
            evaluator.registerImpactResolver(TYPE, recordingProfiles(new ArrayList<>()));
            evaluator.registerProfileResolver(TYPE, (services, asset) -> {
                throw new IllegalStateException("no kubeconfig");
            });

            assertThrows(WombatEvaluationException.class, () -> evaluator.computeImpact(new TestAsset("a1"), mock(ActivityData.class)));
        }
    }

    @Test
    void computeImpact_failsAnAssetThatCannotTakeItsResolvedProfile()
    {
        List<AssetProfile> evaluated = new ArrayList<>();

        try (AssetEvaluator evaluator = new AssetEvaluator(contextOf(List.of()), SERVICES))
        {
            evaluator.registerImpactResolver(TYPE, recordingProfiles(evaluated));
            evaluator.registerProfileResolver(TYPE, (services, asset) -> resolved -> new TestInferredProfile());

            assertThrows(WombatEvaluationException.class, () -> evaluator.computeImpact(new UnpatchableAsset("a1"), mock(ActivityData.class)));
        }

        // Never evaluated with its declared profile under the origin of the resolved one
        assertEquals(List.of(), evaluated);
    }

    @Test
    void computeImpact_tellsWhereAnInferredProfileComesFrom() throws WombatEvaluationException
    {
        try (AssetEvaluator evaluator = new AssetEvaluator(contextOf(List.of()), SERVICES))
        {
            evaluator.registerImpactResolver(TYPE, (services, declared) -> (asset, activity) -> impact(asset));
            evaluator.registerProfileResolver(TYPE, (services, asset) -> resolved -> new TestInferredProfile());

            AssetImpact impact = (AssetImpact) evaluator.computeImpact(new TestAsset("a1"), mock(ActivityData.class)).orElseThrow();

            assertEquals(TestOrigin.INFERRED_FOR_TEST, impact.profileOrigin());
            assertEquals(List.of("a slice"), impact.warnings());
        }
    }

    @Test
    void computeImpact_tellsADeclaredProfileIsDeclared() throws WombatEvaluationException
    {
        try (AssetEvaluator evaluator = new AssetEvaluator(contextOf(List.of()), SERVICES))
        {
            evaluator.registerImpactResolver(TYPE, (services, declared) -> (asset, activity) -> impact(asset));

            AssetImpact impact = (AssetImpact) evaluator.computeImpact(new TestAsset("a1"), mock(ActivityData.class)).orElseThrow();

            assertEquals(ProfileOrigin.DECLARED, impact.profileOrigin());
            assertEquals(List.of(), impact.warnings());
        }
    }

    @Test
    void report_tellsTheAssetsThatFailedOnceEach() throws WombatEvaluationException
    {
        try (AssetEvaluator evaluator = new AssetEvaluator(contextOf(List.of(new TestAsset("a1"), new TestAsset("a2"))), SERVICES))
        {
            evaluator.registerActivityResolver(TYPE, (services, asset) -> (resolved, range, filter) ->
                Optional.of(new AssetActivity(resolved.identity(), List.of(mock(ActivityData.class), mock(ActivityData.class))))
            );
            evaluator.registerImpactResolver(TYPE, (services, declared) -> (asset, activity) -> impact(asset));
            evaluator.registerCostResolver(TYPE, (services, declared) -> (asset, activity) -> impact(asset));
            evaluator.registerProfileResolver(TYPE, (services, declared) -> new WombatProfileResolver() {
                @Override
                public boolean accept(Asset candidate)
                {
                    return candidate.identity().id().equals("a2");
                }

                @Override
                public AssetProfile resolve(Asset candidate) throws WombatEvaluationException
                {
                    throw new WombatEvaluationException("forbidden to list nodes");
                }
            });

            EvaluationReport report = evaluator.report(RANGE, AssetFilter.none());

            assertEquals(4, report.evaluations().size());
            assertEquals(List.of(new AssetEvaluationFailure(new TestAsset("a2").identity(), "forbidden to list nodes")), report.failures());
        }
    }

    private static AssetImpact impact(Asset asset)
    {
        return new AssetImpact(asset.identity(), Footprint.sum(List.of()), List.of(), ImpactProvider.BOAVIZTA, RANGE);
    }

    private static ProcessorFactory<WombatEvaluationResolver> recordingProfiles(List<AssetProfile> evaluated)
    {
        return (services, declared) -> (asset, activity) -> {
            evaluated.add(asset.profile());
            return mock(AssetEvaluation.class);
        };
    }

    private static AssetEvaluator evaluator(List<Asset> assets, List<RecordingResolver> created)
    {
        return new AssetEvaluator(contextOf(assets), SERVICES).registerActivityResolver(TYPE, (services, asset) -> {
            RecordingResolver resolver = new RecordingResolver();
            created.add(resolver);
            return resolver;
        });
    }

    private static WombatContextProvider contextOf(List<Asset> assets)
    {
        return () -> new ResolvedContext(new Environment("env", List.copyOf(assets)));
    }

    /** Resolves no activity: only which assets it was handed, and whether it was closed, matter here. */
    private static final class RecordingResolver implements WombatActivityResolver, AutoCloseable
    {
        private final List<String> resolved = Collections.synchronizedList(new ArrayList<>());
        private final AtomicInteger closed = new AtomicInteger();

        @Override
        public Optional<AssetActivity> resolve(Asset asset, TimeRange range, AssetFilter filter)
        {
            this.resolved.add(asset.identity().id());
            return Optional.empty();
        }

        @Override
        public void close()
        {
            this.closed.incrementAndGet();
        }
    }

    /** The origins a module would define for the profiles it infers. */
    private enum TestOrigin implements ProfileOrigin
    {
        INFERRED_FOR_TEST
    }

    private record TestInferredProfile() implements AssetProfile, InferredProfile
    {
        @Override
        public ProfileOrigin origin()
        {
            return TestOrigin.INFERRED_FOR_TEST;
        }

        @Override
        public List<String> warnings()
        {
            return List.of("a slice");
        }
    }

    private record TestAsset(String assetId, AssetProfile profile) implements PatchableAsset
    {
        TestAsset(String assetId)
        {
            this(assetId, null);
        }

        @Override
        public TestAsset withProfile(AssetProfile replacement)
        {
            return new TestAsset(this.assetId, replacement);
        }

        @Override
        public AssetIdentity identity()
        {
            return AssetIdentity.of(this.assetId, "env", this.type());
        }

        @Override
        public AssetType type()
        {
            return TYPE;
        }
    }

    private record UnpatchableAsset(String assetId) implements Asset
    {
        @Override
        public AssetIdentity identity()
        {
            return AssetIdentity.of(this.assetId, "env", this.type());
        }

        @Override
        public AssetType type()
        {
            return TYPE;
        }

        @Override
        public AssetProfile profile()
        {
            return null;
        }
    }
}
