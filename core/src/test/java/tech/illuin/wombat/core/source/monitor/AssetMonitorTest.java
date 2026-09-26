package tech.illuin.wombat.core.source.monitor;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.ActivityRegime;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetType;
import tech.illuin.wombat.core.asset.Environment;
import tech.illuin.wombat.core.asset.ServiceFamily;
import tech.illuin.wombat.core.asset.profile.Profile;
import tech.illuin.wombat.core.context.ResolvedContext;
import tech.illuin.wombat.core.context.WombatContextProvider;
import tech.illuin.wombat.core.source.AssetMonitor;
import tech.illuin.wombat.core.source.Monitorable;
import tech.illuin.wombat.core.source.WombatSource;
import tech.illuin.wombat.core.source.WombatSourceException;
import tech.illuin.wombat.core.source.data.MetricData;
import tech.illuin.wombat.core.source.persistence.WombatMetricPersister;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetMonitorTest
{
    private static final AssetType TYPE_LLM = AssetType.of("tech.illuin", "wombat-module", "llm-prometheus", ActivityRegime.MEASURED, ServiceFamily.LLM);
    private static final AssetType TYPE_K8S = AssetType.of("tech.illuin", "wombat-module", "kubernetes-api", ActivityRegime.MEASURED, ServiceFamily.KUBERNETES_CONTAINER);

    public static void main(String[] args) throws Exception
    {
        AssetMonitorTest test = new AssetMonitorTest();
        test.sameSourceTasksAreQueuedAndNeverRunSimultaneously();
        System.out.println("PASS: sameSourceTasksAreQueuedAndNeverRunSimultaneously");
        test.differentSourcesRunConcurrently();
        System.out.println("PASS: differentSourcesRunConcurrently");
        test.errorInOneTaskDoesNotHaltSubsequentQueuedTasks();
        System.out.println("PASS: errorInOneTaskDoesNotHaltSubsequentQueuedTasks");
        System.out.println("ALL TESTS PASSED");
    }

    @Test
    void sameSourceTasksAreQueuedAndNeverRunSimultaneously()
    {
        ConcurrentCheckingSource source = new ConcurrentCheckingSource(30);
        TestAsset a1 = new TestAsset("a1", TYPE_LLM, 1);
        TestAsset a2 = new TestAsset("a2", TYPE_LLM, 1);
        TestAsset a3 = new TestAsset("a3", TYPE_LLM, 1);
        TestAsset a4 = new TestAsset("a4", TYPE_LLM, 1);

        try (AssetMonitor monitor = new AssetMonitor(contextOf(a1, a2, a3, a4), noopPersister()))
        {
            monitor.register(a1, source);
            monitor.trigger();
        }

        assertEquals(1, source.maxConcurrent.get());
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

        WombatSource sourceA = (heartbeat, asset) -> {
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

        WombatSource sourceB = (heartbeat, asset) -> {
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

        TestAsset a1 = new TestAsset("a1", TYPE_LLM, 1);
        TestAsset a2 = new TestAsset("a2", TYPE_K8S, 1);

        try (AssetMonitor monitor = new AssetMonitor(contextOf(a1, a2), noopPersister()))
        {
            monitor.register(a1, sourceA);
            monitor.register(a2, sourceB);
            monitor.trigger();
        }

        assertEquals(2, maxConcurrentSources.get());
    }

    @Test
    void errorInOneTaskDoesNotHaltSubsequentQueuedTasks()
    {
        List<String> processed = Collections.synchronizedList(new ArrayList<>());
        AtomicBoolean threw = new AtomicBoolean(false);

        WombatSource source = (heartbeat, asset) -> {
            if ("error-asset".equals(asset.id()))
            {
                threw.set(true);
                throw new WombatSourceException("Simulated failure");
            }
            processed.add(asset.id());
            return Collections.emptyList();
        };

        TestAsset errorAsset = new TestAsset("error-asset", TYPE_LLM, 1);
        TestAsset nextAsset = new TestAsset("next-asset", TYPE_LLM, 1);

        try (AssetMonitor monitor = new AssetMonitor(contextOf(errorAsset, nextAsset), noopPersister()))
        {
            monitor.register(errorAsset, source);
            monitor.trigger();
        }

        assertTrue(threw.get());
        assertEquals(List.of("next-asset"), processed);
    }

    private static WombatContextProvider contextOf(Asset... assets)
    {
        return () -> new ResolvedContext(new Environment("env", List.of(assets)));
    }

    private static WombatMetricPersister noopPersister()
    {
        return metrics -> {};
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
        public List<MetricData> source(Instant heartbeat, Asset asset)
        {
            int current = this.currentConcurrent.incrementAndGet();
            this.maxConcurrent.accumulateAndGet(current, Math::max);
            try {
                Thread.sleep(this.sleepMs);
                this.processed.add(asset.id());
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

    private record TestAsset(String id, AssetType type, int heartbeatSkip) implements Asset, Monitorable
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
        public Profile profile()
        {
            return null;
        }
    }
}
