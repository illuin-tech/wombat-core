package tech.illuin.wombat.core.source;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.Environment;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.asset.profile.AssetProfile;
import tech.illuin.wombat.core.context.ResolvedContext;
import tech.illuin.wombat.core.context.WombatContextProvider;
import tech.illuin.wombat.core.module.WombatServices;
import tech.illuin.wombat.core.secret.SecretResolver;
import tech.illuin.wombat.core.source.data.MetricData;
import tech.illuin.wombat.core.source.persistence.WombatMetricPersister;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetMonitorTest
{
    private static final AssetType TYPE_LLM = AssetType.of("tech.illuin", "wombat-module", "llm-prometheus", ActivityRegime.MEASURED, ServiceFamily.LLM);
    private static final AssetType TYPE_K8S = AssetType.of("tech.illuin", "wombat-module", "kubernetes-api", ActivityRegime.MEASURED, ServiceFamily.KUBERNETES_CONTAINER);
    private static final WombatServices SERVICES = new WombatServices(SecretResolver.standard());

    public static void main(String[] args) throws Exception
    {
        AssetMonitorTest test = new AssetMonitorTest();
        test.sameSourceTasksAreRunSimultaneously();
        System.out.println("PASS: sameSourceTasksAreRunSimultaneously");
        test.differentSourcesRunConcurrently();
        System.out.println("PASS: differentSourcesRunConcurrently");
        test.errorInOneTaskDoesNotHaltSubsequentQueuedTasks();
        System.out.println("PASS: errorInOneTaskDoesNotHaltSubsequentQueuedTasks");
        System.out.println("ALL TESTS PASSED");
    }

    @Test
    void sameSourceTasksAreRunSimultaneously() throws InterruptedException
    {
        ConcurrentCheckingSource source = new ConcurrentCheckingSource(30);
        TestAsset a1 = new TestAsset("a1", TYPE_LLM, Period.ONE_SECOND);
        TestAsset a2 = new TestAsset("a2", TYPE_LLM, Period.ONE_SECOND);
        TestAsset a3 = new TestAsset("a3", TYPE_LLM, Period.ONE_SECOND);
        TestAsset a4 = new TestAsset("a4", TYPE_LLM, Period.ONE_SECOND);

        try (AssetMonitor monitor = new AssetMonitor(contextOf(a1, a2, a3, a4), SERVICES, noopPersister()))
        {
            monitor.register(a1.type(), (services, asset) -> source);
            Thread.sleep(1050);
            monitor.trigger();
        }

        assertEquals(4, source.maxConcurrent.get());
        assertEquals(4, source.processed.size());
        assertTrue(source.processed.containsAll(List.of("a1", "a2", "a3", "a4")));
    }

    @Test
    void differentSourcesRunConcurrently()
    {
        CountDownLatch latchAStarted = new CountDownLatch(1);
        CountDownLatch latchBStarted = new CountDownLatch(1);
        AtomicInteger activeConcurrentSources = new AtomicInteger(0);
        AtomicInteger maxConcurrentSources = new AtomicInteger(0);

        WombatSource sourceA = (heartbeat, lastActivation, asset) -> {
            int current = activeConcurrentSources.incrementAndGet();
            maxConcurrentSources.accumulateAndGet(current, Math::max);
            latchAStarted.countDown();
            try {
                assertTrue(latchBStarted.await(2, TimeUnit.SECONDS));
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            finally {
                activeConcurrentSources.decrementAndGet();
            }
            return Collections.emptyList();
        };

        WombatSource sourceB = (heartbeat, lastActivation, asset) -> {
            int current = activeConcurrentSources.incrementAndGet();
            maxConcurrentSources.accumulateAndGet(current, Math::max);
            latchBStarted.countDown();
            try {
                assertTrue(latchAStarted.await(2, TimeUnit.SECONDS));
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            finally {
                activeConcurrentSources.decrementAndGet();
            }
            return Collections.emptyList();
        };

        TestAsset a1 = new TestAsset("a1", TYPE_LLM, Period.ONE_SECOND);
        TestAsset a2 = new TestAsset("a2", TYPE_K8S, Period.ONE_SECOND);

        try (AssetMonitor monitor = new AssetMonitor(contextOf(a1, a2), SERVICES, noopPersister()))
        {
            monitor.register(a1.type(), (services, asset) -> sourceA);
            monitor.register(a2.type(), (services, asset) -> sourceB);
            monitor.trigger();
        }

        assertEquals(2, maxConcurrentSources.get());
    }

    @Test
    void errorInOneTaskDoesNotHaltSubsequentQueuedTasks()
    {
        List<String> processed = Collections.synchronizedList(new ArrayList<>());
        AtomicBoolean threw = new AtomicBoolean(false);

        WombatSource source = (heartbeat, lastActivation, asset) -> {
            if ("error-asset".equals(asset.identity().id()))
            {
                threw.set(true);
                throw new WombatSourceException("Simulated failure");
            }
            processed.add(asset.identity().id());
            return Collections.emptyList();
        };

        TestAsset errorAsset = new TestAsset("error-asset", TYPE_LLM, Period.ONE_SECOND);
        TestAsset nextAsset = new TestAsset("next-asset", TYPE_LLM, Period.ONE_SECOND);

        try (AssetMonitor monitor = new AssetMonitor(contextOf(errorAsset, nextAsset), SERVICES, noopPersister()))
        {
            monitor.register(errorAsset.type(), (services, asset) -> source);
            monitor.trigger();
        }

        assertTrue(threw.get());
        assertEquals(List.of("next-asset"), processed);
    }

    @Test
    void removedAssetsHaveTheirSourceClosed() throws InterruptedException
    {
        List<Asset> assets = new CopyOnWriteArrayList<>(List.of(new SettingAsset("a1", "v1")));
        RecordingSource source = new RecordingSource();

        try (AssetMonitor monitor = new AssetMonitor(contextOf(assets), SERVICES, noopPersister()))
        {
            monitor.register(TYPE_LLM, (services, asset) -> source);
            monitor.trigger();
            assertNotNull(source.sampled.poll(2, TimeUnit.SECONDS));

            assets.clear();
            monitor.trigger();
            assertTrue(source.closed.await(2, TimeUnit.SECONDS));
        }
    }

    @Test
    void assetsWhoseSettingsChangeKeepTheirSource() throws InterruptedException
    {
        List<Asset> assets = new CopyOnWriteArrayList<>(List.of(new SettingAsset("a1", "v1")));
        AtomicInteger created = new AtomicInteger();
        RecordingSource source = new RecordingSource();

        try (AssetMonitor monitor = new AssetMonitor(contextOf(assets), SERVICES, noopPersister()))
        {
            monitor.register(TYPE_LLM, (services, asset) -> {
                created.incrementAndGet();
                return source;
            });
            monitor.trigger();
            assertNotNull(source.sampled.poll(2, TimeUnit.SECONDS));

            SettingAsset changed = new SettingAsset("a1", "v2");
            assets.set(0, changed);
            monitor.trigger();

            // The source is kept, but it is handed the asset as it currently is.
            assertSame(changed, source.sampled.poll(2, TimeUnit.SECONDS));
            assertEquals(1, created.get());
        }
    }

    @Test
    void refreshedAssetsHaveTheirSourceRebuilt() throws InterruptedException
    {
        SettingAsset a1 = new SettingAsset("a1", "v1");
        BlockingQueue<RecordingSource> created = new LinkedBlockingQueue<>();

        try (AssetMonitor monitor = new AssetMonitor(contextOf(a1), SERVICES, noopPersister()))
        {
            monitor.register(TYPE_LLM, (services, asset) -> {
                RecordingSource source = new RecordingSource();
                created.add(source);
                return source;
            });
            monitor.trigger();
            RecordingSource first = created.poll(2, TimeUnit.SECONDS);
            assertNotNull(first);
            assertNotNull(first.sampled.poll(2, TimeUnit.SECONDS));

            monitor.refresh(a1.identity());
            assertTrue(first.closed.await(0, TimeUnit.SECONDS));

            monitor.trigger();
            RecordingSource second = created.poll(2, TimeUnit.SECONDS);
            assertNotNull(second);
            assertNotNull(second.sampled.poll(2, TimeUnit.SECONDS));
        }
    }

    private static WombatContextProvider contextOf(List<Asset> assets)
    {
        return () -> new ResolvedContext(new Environment("env", List.copyOf(assets)));
    }

    private static WombatContextProvider contextOf(Asset... assets)
    {
        return () -> new ResolvedContext(new Environment("env", List.of(assets)));
    }

    private static WombatMetricPersister noopPersister()
    {
        return (asset, type, metrics) -> {};
    }

    private static final class ConcurrentCheckingSource implements WombatSource
    {
        private final long sleepMs;
        private final AtomicInteger currentConcurrent = new AtomicInteger(0);
        private final AtomicInteger maxConcurrent = new AtomicInteger(0);
        private final List<String> processed = Collections.synchronizedList(new ArrayList<>());

        ConcurrentCheckingSource(long sleepMs)
        {
            this.sleepMs = sleepMs;
        }

        @Override
        public List<MetricData> source(Instant heartbeat, Instant lastActivation, Asset asset)
        {
            int current = this.currentConcurrent.incrementAndGet();
            this.maxConcurrent.accumulateAndGet(current, Math::max);
            try {
                Thread.sleep(this.sleepMs);
                this.processed.add(asset.identity().id());
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            finally {
                this.currentConcurrent.decrementAndGet();
            }
            return Collections.emptyList();
        }
    }

    private static final class RecordingSource implements WombatSource, AutoCloseable
    {
        private final BlockingQueue<Asset> sampled = new LinkedBlockingQueue<>();
        private final CountDownLatch closed = new CountDownLatch(1);

        @Override
        public List<MetricData> source(Instant heartbeat, Instant lastActivation, Asset asset)
        {
            this.sampled.add(asset);
            return Collections.emptyList();
        }

        @Override
        public void close()
        {
            this.closed.countDown();
        }
    }

    /** Triggered on every heartbeat; {@code setting} stands for any property of the asset that is not part of its identity. */
    private record SettingAsset(String assetId, String setting) implements Asset, Monitorable
    {
        @Override
        public AssetIdentity identity()
        {
            return AssetIdentity.of(this.assetId, "env", this.type());
        }

        @Override
        public AssetType type()
        {
            return TYPE_LLM;
        }

        @Override
        public AssetProfile profile()
        {
            return null;
        }

        @Override
        public Period period()
        {
            return Period.ZERO;
        }
    }

    private record TestAsset(String assetId, AssetType type, Period period) implements Asset, Monitorable
    {
        @Override
        public AssetIdentity identity()
        {
            return AssetIdentity.of(this.assetId, "env", this.type());
        }

        @Override
        public AssetProfile profile()
        {
            return null;
        }
    }
}
