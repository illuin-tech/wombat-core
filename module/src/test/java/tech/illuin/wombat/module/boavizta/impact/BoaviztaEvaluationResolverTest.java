package tech.illuin.wombat.module.boavizta.impact;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.activity.kubernetes.CPUUsage;
import tech.illuin.wombat.core.activity.kubernetes.KubernetesActivityData;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.profile.AssetProfile;
import tech.illuin.wombat.core.asset.profile.Lifetime;
import tech.illuin.wombat.core.asset.profile.BareServerProfile;
import tech.illuin.wombat.core.asset.profile.CloudServerProfile;
import tech.illuin.wombat.core.asset.profile.ServerProvider;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.module.boavizta.connector.BoaviztaClient;
import tech.illuin.wombat.module.boavizta.connector.model.BoaviztaImpactResponse;
import tech.illuin.wombat.module.boavizta.connector.model.BoaviztaInstanceConfigResponse;
import tech.illuin.wombat.module.boavizta.connector.model.BoaviztaCloudInstanceImpactRequest;
import tech.illuin.wombat.module.boavizta.connector.model.BoaviztaServerImpactRequest;
import tech.illuin.wombat.module.boavizta.connector.model.BoaviztaServerProvider;
import tech.illuin.wombat.core.evaluation.AssetEvaluation;
import tech.illuin.wombat.core.evaluation.impact.commons.AssetImpact;
import tech.illuin.wombat.core.evaluation.impact.kubernetes.KubernetesImpact;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static tech.illuin.wombat.core.activity.commons.Country.FRANCE;

class BoaviztaEvaluationResolverTest
{
    private static final AssetType K8S_TYPE = AssetType.of("tech.test", "k8s", ActivityRegime.MEASURED, ServiceFamily.KUBERNETES_CONTAINER);

    private final BoaviztaEvaluationResolver resolver = new BoaviztaEvaluationResolver(null);

    @Test
    void accept_kubernetesFamilyWithServerProfile_returnsTrue()
    {
        assertTrue(this.resolver.accept(new TestAsset(K8S_TYPE, new TestServerProfile(ServerProvider.aws, "c5a.4xlarge", FRANCE, Lifetime.ofYears(5), 1))));
    }

    @Test
    void accept_kubernetesFamilyWithPlainProfile_returnsFalse()
    {
        assertFalse(this.resolver.accept(new TestAsset(K8S_TYPE, new AssetProfile() {})));
    }

    @Test
    void accept_kubernetesFamilyWithNullProfile_returnsFalse()
    {
        assertFalse(this.resolver.accept(new TestAsset(K8S_TYPE, null)));
    }

    @Test
    void resolve_relativeUsage_scalesSingleNodeEmissionsByNodeCount() throws Exception
    {
        BoaviztaClient client = mock(BoaviztaClient.class);
        BoaviztaEvaluationResolver evalResolver = new BoaviztaEvaluationResolver(client);

        when(client.getInstanceConfig(eq(BoaviztaServerProvider.aws), eq("c5a.4xlarge")))
            .thenReturn(new BoaviztaInstanceConfigResponse(
                new BoaviztaInstanceConfigResponse.IntConfigItem(16),
                new BoaviztaInstanceConfigResponse.IntConfigItem(32),
                null, null, null, null
            ));

        BoaviztaImpactResponse mockImpact = new BoaviztaImpactResponse(
            Map.of(
                "gwp", new BoaviztaImpactResponse.Impact(
                    "kgCO2eq",
                    "Global Warming Potential",
                    new BoaviztaImpactResponse.Impact.ImpactItem(100.0f, 90.0f, 110.0f),
                    new BoaviztaImpactResponse.Impact.ImpactItem(20.0f, 18.0f, 22.0f)
                )
            ),
            1,
            verbose(20.0)
        );

        when(client.getInstanceImpact(eq(true), anyInt(), anySet(), any(BoaviztaCloudInstanceImpactRequest.class)))
            .thenReturn(mockImpact);

        TestServerProfile profile = new TestServerProfile(ServerProvider.aws, "c5a.4xlarge", FRANCE, Lifetime.ofHours(1000), 3);
        TestAsset asset = new TestAsset(K8S_TYPE, profile);

        Instant start = Instant.parse("2026-11-01T00:00:00Z");
        Instant end = Instant.parse("2026-11-01T01:00:00Z");
        TimeRange range = new TimeRange(start, end, Duration.ofHours(1));

        KubernetesActivityData activity = new KubernetesActivityData(
            ActivityRegime.MODELED,
            Set.of("srv-1"),
            range,
            CPUUsage.relative(0.50),
            Map.of("srv-1", 1.0),
            Map.of()
        );

        AssetEvaluation evaluation = evalResolver.resolve(asset, activity);
        assertInstanceOf(AssetImpact.class, evaluation);
        AssetImpact assetImpact = (AssetImpact) evaluation;

        // Proration factor: 1 hour / 1000 hours = 0.001
        // Single node footprint: embedded = 100 * 0.001 = 0.1, use = 20 * 0.001 = 0.02, total = 0.12
        // Scaled by 3 nodes: embedded = 0.3, use = 0.06, total = 0.36
        assertEquals(0.36, assetImpact.footprint().gwp().totalValue(), 1e-4);
        assertEquals(0.30, assetImpact.footprint().gwp().embedded().value(), 1e-4);
        assertEquals(0.06, assetImpact.footprint().gwp().use().value(), 1e-4);
        // Energy: 20 W per node over the 1 hour range = 0.02 kWh, then 0.06 kWh on 3 nodes
        assertEquals("kWh", assetImpact.footprint().energy().unit());
        assertEquals(0.0, assetImpact.footprint().energy().embedded().value(), 1e-4);
        assertEquals(0.06, assetImpact.footprint().energy().use().value(), 1e-4);

        KubernetesImpact k8sImpact = (KubernetesImpact) assetImpact.serviceImpacts().getFirst();
        assertEquals(50.0, k8sImpact.loadPercent());
        assertEquals(3, k8sImpact.nodeCount());
    }

    /** ISO 3166 has no code for the world, Boavizta names it WOR. */
    @Test
    void resolve_worldLocation_requestsTheWorldAverage() throws Exception
    {
        BoaviztaClient client = mock(BoaviztaClient.class);
        BoaviztaEvaluationResolver evalResolver = new BoaviztaEvaluationResolver(client);

        when(client.getInstanceConfig(eq(BoaviztaServerProvider.aws), eq("c5a.4xlarge")))
            .thenReturn(new BoaviztaInstanceConfigResponse(new BoaviztaInstanceConfigResponse.IntConfigItem(16), null, null, null, null, null));
        when(client.getInstanceImpact(eq(true), anyInt(), anySet(), any(BoaviztaCloudInstanceImpactRequest.class)))
            .thenReturn(new BoaviztaImpactResponse(Map.of(), 0, null));

        TestAsset asset = new TestAsset(K8S_TYPE, new TestServerProfile(ServerProvider.aws, "c5a.4xlarge", Country.WORLD, Lifetime.ofHours(1000), 1));
        TimeRange range = new TimeRange(Instant.parse("2026-11-01T00:00:00Z"), Instant.parse("2026-11-01T01:00:00Z"), Duration.ofHours(1));
        evalResolver.resolve(asset, new KubernetesActivityData(ActivityRegime.MODELED, Set.of("srv-1"), range, CPUUsage.relative(0.50), Map.of("srv-1", 1.0), Map.of()));

        verify(client, atLeastOnce()).getInstanceImpact(eq(true), anyInt(), anySet(), argThat(request -> "WOR".equals(request.usage().usageLocation())));
        verify(client, never()).getInstanceImpact(eq(true), anyInt(), anySet(), argThat(request -> !"WOR".equals(request.usage().usageLocation())));
    }

    /** Boavizta's default lifetime for the instance would otherwise set the embedded share, whatever the profile says. */
    @Test
    void resolve_lifespan_setsTheHardwareLifetime() throws Exception
    {
        BoaviztaClient client = mock(BoaviztaClient.class);
        BoaviztaEvaluationResolver evalResolver = new BoaviztaEvaluationResolver(client);

        when(client.getInstanceConfig(eq(BoaviztaServerProvider.aws), eq("c5a.4xlarge")))
            .thenReturn(new BoaviztaInstanceConfigResponse(new BoaviztaInstanceConfigResponse.IntConfigItem(16), null, null, null, null, null));
        when(client.getInstanceImpact(eq(true), anyInt(), anySet(), any(BoaviztaCloudInstanceImpactRequest.class)))
            .thenReturn(new BoaviztaImpactResponse(Map.of(), 0, null));

        TestAsset asset = new TestAsset(K8S_TYPE, new TestServerProfile(ServerProvider.aws, "c5a.4xlarge", FRANCE, Lifetime.ofYears(3), 1));
        TimeRange range = new TimeRange(Instant.parse("2026-11-01T00:00:00Z"), Instant.parse("2026-11-01T01:00:00Z"), Duration.ofHours(1));
        evalResolver.resolve(asset, new KubernetesActivityData(ActivityRegime.MODELED, Set.of("srv-1"), range, CPUUsage.relative(0.50), Map.of("srv-1", 1.0), Map.of()));

        long lifespanHours = Lifetime.ofYears(3).asHours();
        verify(client).getInstanceImpact(eq(true), eq((int) lifespanHours), anySet(), argThat(request -> request.usage().hoursLifeTime() == lifespanHours));
    }

    @Test
    void accept_kubernetesFamilyWithBareServerProfile_returnsTrue()
    {
        assertTrue(this.resolver.accept(new TestAsset(K8S_TYPE, bareServer(null, null))));
    }

    @Test
    void resolve_bareServer_sendsItsConfigurationToTheServerEndpoint() throws Exception
    {
        BoaviztaClient client = mock(BoaviztaClient.class);
        BoaviztaEvaluationResolver evalResolver = new BoaviztaEvaluationResolver(client);

        when(client.getServerImpact(eq(true), anyInt(), anySet(), any(BoaviztaServerImpactRequest.class)))
            .thenReturn(new BoaviztaImpactResponse(Map.of(), 0, null));

        TestBareServerProfile profile = bareServer(new BareServerProfile.Disk(2, 960), new BareServerProfile.GPU(1, "NVIDIA A100"));
        TimeRange range = new TimeRange(Instant.parse("2026-11-01T00:00:00Z"), Instant.parse("2026-11-01T01:00:00Z"), Duration.ofHours(1));
        evalResolver.resolve(new TestAsset(K8S_TYPE, profile), new KubernetesActivityData(ActivityRegime.MODELED, Set.of("srv-1"), range, CPUUsage.relative(0.50), Map.of("srv-1", 1.0), Map.of()));

        long lifespanHours = Lifetime.ofYears(5).asHours();
        BoaviztaServerImpactRequest.Configuration expected = new BoaviztaServerImpactRequest.Configuration(
            new BoaviztaServerImpactRequest.Configuration.CPU(2, 24, "Intel Xeon Gold 6342"),
            List.of(new BoaviztaServerImpactRequest.Configuration.RAM(8, 32)),
            List.of(new BoaviztaServerImpactRequest.Configuration.Disk(2, "ssd", 960)),
            new BoaviztaServerImpactRequest.Configuration.GPU(1, "NVIDIA A100")
        );
        verify(client).getServerImpact(eq(true), eq((int) lifespanHours), anySet(), argThat(request ->
            expected.equals(request.configuration())
                && request.usage().hoursLifeTime() == lifespanHours
                && "FRA".equals(request.usage().usageLocation())
                && request.usage().timeWorkload().getFirst().loadPercentage() == 50.0
        ));
        verifyNoMoreInteractions(client);
    }

    @Test
    void resolve_bareServerNanocores_readsTheVcpuFromTheProfile() throws Exception
    {
        BoaviztaClient client = mock(BoaviztaClient.class);
        BoaviztaEvaluationResolver evalResolver = new BoaviztaEvaluationResolver(client);

        when(client.getServerImpact(eq(true), anyInt(), anySet(), any(BoaviztaServerImpactRequest.class)))
            .thenReturn(new BoaviztaImpactResponse(Map.of(), 0, null));

        TimeRange range = new TimeRange(Instant.parse("2026-11-01T00:00:00Z"), Instant.parse("2026-11-01T01:00:00Z"), Duration.ofHours(1));
        AssetImpact impact = (AssetImpact) evalResolver.resolve(
            new TestAsset(K8S_TYPE, bareServer(null, null)),
            new KubernetesActivityData(ActivityRegime.MEASURED, Set.of("srv-1"), range, CPUUsage.nanocores(48_000_000_000.0), Map.of("srv-1", 1.0), Map.of())
        );

        KubernetesImpact k8sImpact = (KubernetesImpact) impact.serviceImpacts().getFirst();
        assertEquals(50.0, k8sImpact.loadPercent(), 1e-9);
        assertEquals(1, k8sImpact.nodeCount());
        assertEquals(96, k8sImpact.serverSpecification().vcpu());
        assertEquals(256, k8sImpact.serverSpecification().memory());
        assertNull(k8sImpact.serverSpecification().ssdStorage());
        verify(client).getServerImpact(eq(true), anyInt(), anySet(), argThat(request -> request.usage().timeWorkload().getFirst().loadPercentage() == 50.0));
        verifyNoMoreInteractions(client);
    }

    /** Past one node, Boavizta is asked for a full node and the remainder, whose powers add up. */
    @Test
    void resolve_nanocoresOverOneNode_addsTheNodesEnergy() throws Exception
    {
        BoaviztaClient client = mock(BoaviztaClient.class);
        BoaviztaEvaluationResolver evalResolver = new BoaviztaEvaluationResolver(client);

        when(client.getServerImpact(eq(true), anyInt(), anySet(), any(BoaviztaServerImpactRequest.class)))
            .thenAnswer(invocation -> {
                BoaviztaServerImpactRequest request = invocation.getArgument(3);
                double load = request.usage().timeWorkload().getFirst().loadPercentage();
                /* Service impacts are sorted by GWP, which the response must hold */
                BoaviztaImpactResponse.Impact.ImpactItem item = new BoaviztaImpactResponse.Impact.ImpactItem(1.0f, 1.0f, 1.0f);
                return new BoaviztaImpactResponse(Map.of("gwp", new BoaviztaImpactResponse.Impact("kgCO2eq", "GWP", item, item)), 0, verbose(load == 100.0 ? 30.0 : 20.0));
            });

        TimeRange range = new TimeRange(Instant.parse("2026-11-01T00:00:00Z"), Instant.parse("2026-11-01T02:00:00Z"), Duration.ofHours(1));
        AssetImpact impact = (AssetImpact) evalResolver.resolve(
            new TestAsset(K8S_TYPE, bareServer(null, null)),
            new KubernetesActivityData(ActivityRegime.MEASURED, Set.of("srv-1", "srv-2"), range, CPUUsage.nanocores(144_000_000_000.0), Map.of("srv-1", 0.75, "srv-2", 0.25), Map.of())
        );

        // 96 vCPU at 144 cores: a full node at 30 W and one at 50% at 20 W, so 50 W over 2 hours = 0.1 kWh
        assertEquals(0.1, impact.footprint().energy().use().value(), 1e-6);
        KubernetesImpact srv2 = (KubernetesImpact) impact.serviceImpacts().stream().filter(si -> si.serviceId().equals("srv-2")).findFirst().orElseThrow();
        assertEquals(0.025, srv2.footprint().energy().use().value(), 1e-6);
    }

    private static BoaviztaImpactResponse.Verbose verbose(double avgPower)
    {
        return new BoaviztaImpactResponse.Verbose(new BoaviztaImpactResponse.Verbose.Value(avgPower), new BoaviztaImpactResponse.Verbose.Value(1.0));
    }

    private static TestBareServerProfile bareServer(BareServerProfile.Disk ssd, BareServerProfile.GPU gpu)
    {
        return new TestBareServerProfile(
            FRANCE, Lifetime.ofYears(5),
            new BareServerProfile.CPU(2, 24, 2, "Intel Xeon Gold 6342"), new BareServerProfile.RAM(8, 32),
            ssd, null, gpu
        );
    }

    private record TestServerProfile(
        ServerProvider provider,
        String instanceType,
        Country location,
        Lifetime lifespan,
        int nodeCount
    ) implements CloudServerProfile {}

    private record TestBareServerProfile(
        Country location,
        Lifetime lifespan,
        CPU cpu,
        RAM ram,
        Disk ssd,
        Disk hdd,
        GPU gpu
    ) implements BareServerProfile {}

    private record TestAsset(AssetType type, AssetProfile profile) implements Asset
    {
        @Override
        public AssetIdentity identity()
        {
            return AssetIdentity.of("asset", "env", this.type());
        }
    }
}
