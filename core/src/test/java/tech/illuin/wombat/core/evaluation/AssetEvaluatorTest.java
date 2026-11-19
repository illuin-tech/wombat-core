package tech.illuin.wombat.core.evaluation;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.activity.commons.AssetActivity;
import tech.illuin.wombat.core.activity.commons.AssetFilter;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.Environment;
import tech.illuin.wombat.core.asset.profile.AssetProfile;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.context.ResolvedContext;
import tech.illuin.wombat.core.context.WombatContextProvider;
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

class AssetEvaluatorTest
{
    private static final AssetType TYPE = AssetType.of("tech.illuin", "wombat-module", "test", ActivityRegime.MODELED, ServiceFamily.LLM);
    private static final WombatServices SERVICES = new WombatServices(SecretResolver.standard());
    private static final TimeRange RANGE = new TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1_000));

    @Test
    void evaluate_resolvesAssetsAddedAfterItsCreation() throws WombatEvaluationException
    {
        List<Asset> assets = new CopyOnWriteArrayList<>();
        List<RecordingResolver> created = new CopyOnWriteArrayList<>();

        try (AssetEvaluator evaluator = evaluator(assets, created))
        {
            evaluator.evaluate(RANGE, AssetFilter.none());
            assets.add(new TestAsset("a1"));
            evaluator.evaluate(RANGE, AssetFilter.none());
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
            evaluator.evaluate(RANGE, AssetFilter.none());
            assets.removeIf(asset -> asset.identity().id().equals("a1"));
            evaluator.evaluate(RANGE, AssetFilter.none());

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
            evaluator.evaluate(RANGE, AssetFilter.none());
            evaluator.refresh(asset.identity());
            evaluator.evaluate(RANGE, AssetFilter.none());

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
            evaluator.evaluate(RANGE, AssetFilter.none());
        }

        assertEquals(List.of("evaluator-test"), threads);
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

    private record TestAsset(String assetId) implements Asset
    {
        @Override
        public AssetIdentity identity()
        {
            return AssetIdentity.of(this.assetId, "env", this.assetId, this.type());
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
