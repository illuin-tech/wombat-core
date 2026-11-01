package tech.illuin.wombat.module.kubernetes_static;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.commons.AssetActivity;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.activity.kubernetes.KubernetesActivityData;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.profile.Lifetime;
import tech.illuin.wombat.core.asset.profile.ServerProvider;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.module.kubernetes_static.activity.KubernetesStaticActivityResolver;
import tech.illuin.wombat.module.kubernetes_static.scenario.BatchActivityScenario;
import tech.illuin.wombat.module.kubernetes_static.scenario.ConstantScenario;
import tech.illuin.wombat.module.kubernetes_static.scenario.UserActivityScenario;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class KubernetesStaticActivityResolverTest
{
    private final KubernetesStaticActivityResolver resolver = new KubernetesStaticActivityResolver();

    @Test
    void resolvesConstantLoadIntoNanocores()
    {
        KubernetesStaticAsset asset = new KubernetesStaticAsset(
            new AssetIdentity("k8s-static-1", "env-test", "Static Cluster 1"),
            new KubernetesStaticProfile(
                ServerProvider.aws,
                "m5.xlarge",
                Country.FRANCE,
                Lifetime.ofYears(5),
                ZoneOffset.UTC,
                List.of(new ConstantScenario(0.25, null))
            )
        );

        Instant start = Instant.parse("2026-11-01T00:00:00Z");
        Instant end = Instant.parse("2026-11-01T02:00:00Z");
        TimeRange range = new TimeRange(start, end, Duration.ofHours(1));

        Optional<AssetActivity> result = this.resolver.resolve(asset, range, null);
        assertTrue(result.isPresent());

        AssetActivity activity = result.get();
        assertEquals(asset.identity(), activity.assetIdentity());
        assertEquals(2, activity.activity().size());

        for (var actData : activity.activity())
        {
            assertInstanceOf(KubernetesActivityData.class, actData);
            KubernetesActivityData k8sData = (KubernetesActivityData) actData;
            assertEquals(ActivityRegime.MODELED, k8sData.regime());
            assertEquals(250_000_000.0, k8sData.cpuUsage(), 1e-4);
            assertEquals(1.0, k8sData.containerShares().get("k8s-static-1"));
        }
    }

    @Test
    void resolvesSuperposedScenariosAcrossTimeSlots()
    {
        // Combine baseline (0.10), diurnal (0.10 -> 0.75, 09:00 - 18:00), batch (0.0 -> 0.90, 02:00 - 04:00)
        KubernetesStaticAsset asset = new KubernetesStaticAsset(
            new AssetIdentity("k8s-static-2", "env-test", "Static Cluster 2"),
            new KubernetesStaticProfile(
                ServerProvider.aws,
                "c5.2xlarge",
                Country.GERMANY,
                Lifetime.ofYears(4),
                ZoneOffset.UTC,
                List.of(
                    new ConstantScenario(0.10, null),
                    new UserActivityScenario(0.10, 0.75, LocalTime.of(9, 0), LocalTime.of(18, 0), UserActivityScenario.CurveShape.SINUSOIDAL, 0.0, null),
                    new BatchActivityScenario(0.0, 0.90, LocalTime.of(2, 0), Duration.ofHours(2), 0.0, BatchActivityScenario.SmoothingMode.NONE, null)
                )
            )
        );

        Instant start = Instant.parse("2026-11-01T00:00:00Z");
        Instant end = Instant.parse("2026-11-02T00:00:00Z");
        TimeRange range = new TimeRange(start, end, Duration.ofHours(1));

        Optional<AssetActivity> result = this.resolver.resolve(asset, range, null);
        assertTrue(result.isPresent());

        List<KubernetesActivityData> slots = result.get().activity().stream()
            .map(a -> (KubernetesActivityData) a)
            .toList();

        assertEquals(24, slots.size());

        // Slot 2: 02:00-03:00 -> batch active (0.90 fraction = 900_000_000 nanocores)
        assertEquals(900_000_000.0, slots.get(2).cpuUsage(), 1e-4);

        // Slot 13: 13:00-14:00 (midpoint 13:30) -> peak user activity (0.75 fraction = 750_000_000 nanocores)
        assertEquals(750_000_000.0, slots.get(13).cpuUsage(), 1e-4);

        // Slot 22: 22:00-23:00 -> constant baseline (0.10 fraction = 100_000_000 nanocores)
        assertEquals(100_000_000.0, slots.get(22).cpuUsage(), 1e-4);
    }

    @Test
    void scalesActivityAcrossMultiNodeCluster()
    {
        KubernetesStaticAsset asset = new KubernetesStaticAsset(
            new AssetIdentity("k8s-static-3", "env-test", "Multi-Node Static Cluster"),
            new KubernetesStaticProfile(
                ServerProvider.aws,
                "m5.2xlarge",
                Country.FRANCE,
                Lifetime.ofYears(5),
                3,
                ZoneOffset.UTC,
                List.of(new ConstantScenario(0.50, null))
            )
        );

        Instant start = Instant.parse("2026-11-01T00:00:00Z");
        Instant end = Instant.parse("2026-11-01T01:00:00Z");
        TimeRange range = new TimeRange(start, end, Duration.ofHours(1));

        Optional<AssetActivity> result = this.resolver.resolve(asset, range, null);
        assertTrue(result.isPresent());

        AssetActivity activity = result.get();
        assertEquals(1, activity.activity().size());

        KubernetesActivityData k8sData = (KubernetesActivityData) activity.activity().get(0);
        assertEquals(ActivityRegime.MODELED, k8sData.regime());
        // 0.50 load * 3 nodes * 1_000_000_000 = 1_500_000_000 nanocores (1.5 cores)
        assertEquals(1_500_000_000.0, k8sData.cpuUsage(), 1e-4);
    }
}
