package tech.illuin.wombat.core.module;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.profile.AssetProfile;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.type.FamilyType;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.evaluation.impact.kubernetes.KubernetesMetricResolver;
import tech.illuin.wombat.core.evaluation.impact.llm.LLMMetricResolver;
import tech.illuin.wombat.core.secret.SecretResolver;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ProcessorPoolTest
{
    private static final AssetType TYPE = AssetType.of("tech.illuin", "wombat-module", "test", ActivityRegime.MEASURED, ServiceFamily.LLM);
    private static final AssetType TYPE_UNSUPPORTED = AssetType.of("tech.illuin", "wombat-module", "unsupported", ActivityRegime.MODELED, ServiceFamily.LLM);
    private static final FamilyType FAMILY_FIRST = FamilyType.of("tech.illuin", "wombat-module", "first", ServiceFamily.LLM);
    private static final FamilyType FAMILY_SECOND = FamilyType.of("tech.illuin", "wombat-module", "second", ServiceFamily.LLM);
    private static final FamilyType FAMILY_OTHER = FamilyType.of("tech.illuin", "wombat-module", "other", ServiceFamily.KUBERNETES_CONTAINER);
    private static final WombatServices SERVICES = new WombatServices(
        SecretResolver.standard(),
        new MetricResolvers(mock(KubernetesMetricResolver.class), mock(LLMMetricResolver.class)),
        List.of()
    );

    @Test
    void get_buildsOnFirstUseAndReusesForTheSameIdentity()
    {
        CountingFactory factory = new CountingFactory();
        try (ProcessorPool<TestProcessor> pool = pool(factory))
        {
            TestProcessor first = pool.get(asset("a1", "v1")).orElseThrow();
            // Same identity, other settings: the processor is kept, it reads the current asset at runtime.
            TestProcessor second = pool.get(asset("a1", "v2")).orElseThrow();

            assertSame(first, second);
            assertEquals(1, factory.created.get());
        }
    }

    @Test
    void get_buildsOneProcessorPerIdentity()
    {
        CountingFactory factory = new CountingFactory();
        try (ProcessorPool<TestProcessor> pool = pool(factory))
        {
            TestProcessor a1 = pool.get(asset("a1", "v1")).orElseThrow();
            TestProcessor a2 = pool.get(asset("a2", "v1")).orElseThrow();

            assertNotSame(a1, a2);
            assertEquals(2, factory.created.get());
        }
    }

    @Test
    void get_handsTheServicesAndAssetToTheFactory()
    {
        Asset asset = asset("a1", "v1");
        try (ProcessorPool<TestProcessor> pool = pool((services, a) -> {
            assertSame(SERVICES, services);
            assertSame(asset, a);
            return new TestProcessor();
        }))
        {
            assertTrue(pool.get(asset).isPresent());
        }
    }

    @Test
    void get_isEmptyWhenNoFactoryIsRegisteredForTheAssetType()
    {
        try (ProcessorPool<TestProcessor> pool = pool(new CountingFactory()))
        {
            assertTrue(pool.registered(TYPE));
            assertFalse(pool.registered(TYPE_UNSUPPORTED));
            assertFalse(pool.supports(new TestAsset("a1", TYPE_UNSUPPORTED, "v1")));
            assertTrue(pool.get(new TestAsset("a1", TYPE_UNSUPPORTED, "v1")).isEmpty());
        }
    }

    @Test
    void get_triesTheAssetTypeBeforeItsFamilyInRegistrationOrder()
    {
        try (ProcessorPool<TestProcessor> pool = new ProcessorPool<TestProcessor>("test-processor", SERVICES)
            .register(FAMILY_FIRST, (services, asset) -> new LabeledProcessor("first", true))
            .register(TYPE, (services, asset) -> new LabeledProcessor("own", true))
            .register(FAMILY_SECOND, (services, asset) -> new LabeledProcessor("second", true)))
        {
            assertEquals("own", ((LabeledProcessor) pool.get(asset("a1", "v1")).orElseThrow()).label);
            // An asset-type without a module of its own is served by its family, the first registered winning.
            assertEquals("first", ((LabeledProcessor) pool.get(new TestAsset("a2", TYPE_UNSUPPORTED, "v1")).orElseThrow()).label);
        }
    }

    @Test
    void get_fallsThroughToTheFamilyWhenACandidateRejectsTheAsset()
    {
        LabeledProcessor rejecting = new LabeledProcessor("own", false);
        try (ProcessorPool<TestProcessor> pool = new ProcessorPool<TestProcessor>("test-processor", SERVICES)
            .register(TYPE, (services, asset) -> rejecting)
            .register(FAMILY_FIRST, (services, asset) -> new LabeledProcessor("first", false))
            .register(FAMILY_SECOND, (services, asset) -> new LabeledProcessor("second", true)))
        {
            assertEquals("second", ((LabeledProcessor) pool.get(asset("a1", "v1")).orElseThrow()).label);
            assertEquals(1, rejecting.closed.get());
        }
    }

    @Test
    void get_ignoresTheModulesOfOtherFamilies()
    {
        try (ProcessorPool<TestProcessor> pool = new ProcessorPool<TestProcessor>("test-processor", SERVICES)
            .register(FAMILY_OTHER, (services, asset) -> new LabeledProcessor("other", true)))
        {
            assertFalse(pool.supports(asset("a1", "v1")));
            assertTrue(pool.get(asset("a1", "v1")).isEmpty());
        }
    }

    @Test
    void get_keepsTheOutcomeWhenNoCandidateAcceptsTheAsset()
    {
        AtomicInteger created = new AtomicInteger();
        try (ProcessorPool<TestProcessor> pool = new ProcessorPool<TestProcessor>("test-processor", SERVICES)
            .register(FAMILY_FIRST, (services, asset) -> {
                created.incrementAndGet();
                return new LabeledProcessor("first", false);
            }))
        {
            Asset asset = asset("a1", "v1");
            assertTrue(pool.supports(asset));
            assertTrue(pool.get(asset).isEmpty());
            assertTrue(pool.get(asset).isEmpty());
            assertEquals(1, created.get());

            // The asset may have changed: a refresh chooses again.
            pool.invalidate(asset.identity());
            assertTrue(pool.get(asset).isEmpty());
            assertEquals(2, created.get());
        }
    }

    @Test
    void registered_tellsFamilyModulesApart()
    {
        try (ProcessorPool<TestProcessor> pool = new ProcessorPool<TestProcessor>("test-processor", SERVICES)
            .register(FAMILY_FIRST, new CountingFactory()))
        {
            assertTrue(pool.registered(FAMILY_FIRST));
            assertFalse(pool.registered(FAMILY_SECOND));
        }
    }

    @Test
    void get_propagatesFactoryFailuresAndRetriesOnTheNextCall()
    {
        AtomicInteger attempts = new AtomicInteger();
        try (ProcessorPool<TestProcessor> pool = pool((services, asset) -> {
            if (attempts.incrementAndGet() == 1)
                throw new IllegalStateException("misconfigured");
            return new TestProcessor();
        }))
        {
            assertThrows(IllegalStateException.class, () -> pool.get(asset("a1", "v1")));
            assertTrue(pool.get(asset("a1", "v1")).isPresent());
            assertEquals(2, attempts.get());
        }
    }

    @Test
    void get_rejectsAFactoryReturningNull()
    {
        try (ProcessorPool<TestProcessor> pool = pool((services, asset) -> null))
        {
            assertThrows(NullPointerException.class, () -> pool.get(asset("a1", "v1")));
        }
    }

    @Test
    void retain_closesTheProcessorsOfAssetsNoLongerRetained()
    {
        CountingFactory factory = new CountingFactory(true);
        try (ProcessorPool<TestProcessor> pool = pool(factory))
        {
            Asset a1 = asset("a1", "v1");
            Asset a2 = asset("a2", "v1");
            ClosingProcessor p1 = (ClosingProcessor) pool.get(a1).orElseThrow();
            ClosingProcessor p2 = (ClosingProcessor) pool.get(a2).orElseThrow();

            pool.retain(Set.of(a2.identity()));

            assertEquals(1, p1.closed.get());
            assertEquals(0, p2.closed.get());
            assertSame(p2, pool.get(a2).orElseThrow());
            assertNotSame(p1, pool.get(a1).orElseThrow());
        }
    }

    @Test
    void retain_discardsProcessorsThatAreNotCloseable()
    {
        CountingFactory factory = new CountingFactory();
        try (ProcessorPool<TestProcessor> pool = pool(factory))
        {
            TestProcessor first = pool.get(asset("a1", "v1")).orElseThrow();

            pool.retain(Set.of());

            assertNotSame(first, pool.get(asset("a1", "v1")).orElseThrow());
            assertEquals(2, factory.created.get());
        }
    }

    @Test
    void invalidate_closesTheProcessorAndRebuildsItOnTheNextCall()
    {
        CountingFactory factory = new CountingFactory(true);
        try (ProcessorPool<TestProcessor> pool = pool(factory))
        {
            Asset asset = asset("a1", "v1");
            ClosingProcessor first = (ClosingProcessor) pool.get(asset).orElseThrow();

            pool.invalidate(asset.identity());

            assertEquals(1, first.closed.get());
            assertNotSame(first, pool.get(asset).orElseThrow());
            assertEquals(2, factory.created.get());
        }
    }

    @Test
    void invalidate_doesNothingWhenNoProcessorWasBuilt()
    {
        CountingFactory factory = new CountingFactory(true);
        try (ProcessorPool<TestProcessor> pool = pool(factory))
        {
            assertDoesNotThrow(() -> pool.invalidate(asset("a1", "v1").identity()));
            assertEquals(0, factory.created.get());
        }
    }

    @Test
    void close_closesEveryProcessorEvenWhenOneFailsToClose()
    {
        ProcessorPool<TestProcessor> pool = pool((services, asset) -> asset.identity().id().equals("failing")
            ? new FailingProcessor()
            : new ClosingProcessor()
        );
        ClosingProcessor a1 = (ClosingProcessor) pool.get(asset("a1", "v1")).orElseThrow();
        pool.get(asset("failing", "v1")).orElseThrow();
        ClosingProcessor a2 = (ClosingProcessor) pool.get(asset("a2", "v1")).orElseThrow();

        assertDoesNotThrow(pool::close);

        assertEquals(1, a1.closed.get());
        assertEquals(1, a2.closed.get());
    }

    private static ProcessorPool<TestProcessor> pool(ProcessorFactory<TestProcessor> factory)
    {
        return new ProcessorPool<TestProcessor>("test-processor", SERVICES).register(TYPE, factory);
    }

    private static Asset asset(String id, String setting)
    {
        return new TestAsset(id, TYPE, setting);
    }

    /** {@code setting} stands for any property of the asset that is not part of its identity. */
    private record TestAsset(String assetId, AssetType type, String setting) implements Asset
    {
        @Override
        public AssetIdentity identity()
        {
            return AssetIdentity.of(this.assetId, "env", this.type);
        }

        @Override
        public AssetProfile profile()
        {
            return PROFILE;
        }
    }

    private static final AssetProfile PROFILE = new AssetProfile() {};

    private static final class CountingFactory implements ProcessorFactory<TestProcessor>
    {
        private final boolean closeable;
        private final AtomicInteger created = new AtomicInteger();

        CountingFactory()
        {
            this(false);
        }

        CountingFactory(boolean closeable)
        {
            this.closeable = closeable;
        }

        @Override
        public TestProcessor create(WombatServices services, Asset asset)
        {
            this.created.incrementAndGet();
            return this.closeable ? new ClosingProcessor() : new TestProcessor();
        }
    }

    private static class TestProcessor implements AssetProcessor {}

    private static class ClosingProcessor extends TestProcessor implements AutoCloseable
    {
        protected final AtomicInteger closed = new AtomicInteger();

        @Override
        public void close()
        {
            this.closed.incrementAndGet();
        }
    }

    /** Tells which module built it, and whether it accepts the assets it is offered. */
    private static final class LabeledProcessor extends ClosingProcessor
    {
        private final String label;
        private final boolean accepts;

        LabeledProcessor(String label, boolean accepts)
        {
            this.label = label;
            this.accepts = accepts;
        }

        @Override
        public boolean accept(Asset asset)
        {
            return this.accepts;
        }
    }

    private static final class FailingProcessor extends TestProcessor implements AutoCloseable
    {
        @Override
        public void close() throws Exception
        {
            throw new Exception("close failed");
        }
    }
}
