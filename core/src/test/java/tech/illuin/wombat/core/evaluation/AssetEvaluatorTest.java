package tech.illuin.wombat.core.evaluation;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.WombatActivityException;
import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.commons.AssetFilter;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.Environment;
import tech.illuin.wombat.core.asset.profile.AssetProfile;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.context.ResolvedContext;
import tech.illuin.wombat.core.evaluation.impact.commons.AssetImpact;
import tech.illuin.wombat.core.evaluation.impact.commons.ImpactProvider;
import tech.illuin.wombat.core.evaluation.impact.llm.LLMImpact;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static tech.illuin.wombat.core.activity.commons.Country.FRANCE;

class AssetEvaluatorTest
{
    private static final AssetType EXTERNAL_TYPE = AssetType.of("tech.test", "llm-external", ActivityRegime.MEASURED, ServiceFamily.LLM);
    private static final AssetType INTERNAL_TYPE = AssetType.of("tech.test", "llm-internal", ActivityRegime.MEASURED, ServiceFamily.LLM);
    private static final TimeRange RANGE = new TimeRange(Instant.EPOCH, Instant.EPOCH.plusSeconds(7200));
    private static final long HOUR_MS = 3_600_000L;

    private final TestAsset prodAsset = new TestAsset(AssetIdentity.of("asset", "prod", "Prod"), EXTERNAL_TYPE);
    private final TestAsset devAsset = new TestAsset(AssetIdentity.of("asset", "dev", "Dev"), INTERNAL_TYPE);
    private final AtomicInteger contextLoads = new AtomicInteger();
    private final ExternalResolver externalResolver = new ExternalResolver();
    private final AssetEvaluator evaluator = new AssetEvaluator(() -> {
        this.contextLoads.incrementAndGet();
        return new ResolvedContext(
            new Environment("prod", List.of(this.prodAsset)),
            new Environment("dev", List.of(this.devAsset))
        );
    })
        .registerActivityResolver(EXTERNAL_TYPE, this.externalResolver)
        .registerActivityResolver(INTERNAL_TYPE, (asset, range, filter) -> Optional.empty());

    @Test
    void externalAssets_getTheirPerBucketActivityFromTheirResolver() throws WombatActivityException
    {
        Optional<Map<Long, Double>> activity = this.evaluator.activityPerBucket(impact("prod", EXTERNAL_TYPE), RANGE, HOUR_MS, List.of("claude"));

        assertEquals(Map.of(HOUR_MS, 1.0), activity.orElseThrow());
        assertEquals(List.of(new Call(this.prodAsset, RANGE, HOUR_MS, List.of("claude"))), this.externalResolver.calls);
    }

    @Test
    void internalAssets_haveNoPerBucketActivityAndAreNotLookedUp() throws WombatActivityException
    {
        assertTrue(this.evaluator.activityPerBucket(impact("dev", INTERNAL_TYPE), RANGE, HOUR_MS, List.of("claude")).isEmpty());
        assertEquals(0, this.contextLoads.get());
    }

    @Test
    void unregisteredTypes_haveNoPerBucketActivity() throws WombatActivityException
    {
        AssetType unknown = AssetType.of("tech.test", "other", ActivityRegime.MEASURED, ServiceFamily.LLM);

        assertTrue(this.evaluator.activityPerBucket(impact("prod", unknown), RANGE, HOUR_MS, List.of("claude")).isEmpty());
    }

    @Test
    void missingAssets_haveNoPerBucketActivity() throws WombatActivityException
    {
        assertTrue(this.evaluator.activityPerBucket(impact("staging", EXTERNAL_TYPE), RANGE, HOUR_MS, List.of("claude")).isEmpty());
        assertTrue(this.externalResolver.calls.isEmpty());
    }

    private static AssetImpact impact(String environmentId, AssetType type)
    {
        LLMImpact service = new LLMImpact("claude", type, null, LLMProvider.anthropic, "claude", FRANCE, null, null, 100L, 1.0);
        return new AssetImpact(environmentId, "asset", null, List.of(service), ImpactProvider.ECOLOGITS);
    }

    private record TestAsset(AssetIdentity identity, AssetType type) implements Asset
    {
        @Override
        public AssetProfile profile()
        {
            return null;
        }
    }

    private record Call(Asset asset, TimeRange range, long stepMs, Collection<String> serviceIds) {}

    private static class ExternalResolver implements WombatActivityResolver
    {
        private final List<Call> calls = new ArrayList<>();

        @Override
        public Optional<ActivityData> resolve(Asset asset, TimeRange range, AssetFilter filter)
        {
            return Optional.empty();
        }

        @Override
        public ActivityResolverType activityResolverType()
        {
            return ActivityResolverType.EXTERNAL_API;
        }

        @Override
        public Map<Long, Double> activityPerBucket(Asset asset, TimeRange range, long stepMs, Collection<String> serviceIds)
        {
            this.calls.add(new Call(asset, range, stepMs, serviceIds));
            return Map.of(HOUR_MS, 1.0);
        }
    }
}
