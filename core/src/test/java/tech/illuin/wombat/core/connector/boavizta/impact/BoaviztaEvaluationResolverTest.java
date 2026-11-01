package tech.illuin.wombat.core.connector.boavizta.impact;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.activity.kubernetes.CPUUsage;
import tech.illuin.wombat.core.activity.kubernetes.KubernetesActivityData;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.profile.AssetProfile;
import tech.illuin.wombat.core.asset.profile.Lifetime;
import tech.illuin.wombat.core.asset.profile.ServerProfile;
import tech.illuin.wombat.core.asset.profile.ServerProvider;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.connector.boavizta.connector.BoaviztaClient;
import tech.illuin.wombat.core.connector.boavizta.connector.model.BoaviztaImpactResponse;
import tech.illuin.wombat.core.connector.boavizta.connector.model.BoaviztaInstanceConfigResponse;
import tech.illuin.wombat.core.connector.boavizta.connector.model.BoaviztaInstanceImpactRequest;
import tech.illuin.wombat.core.connector.boavizta.connector.model.BoaviztaServerProvider;
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
                    new BoaviztaImpactResponse.Impact.ImpactItem(100.0f, 90.0f, 110.0f, List.of()),
                    new BoaviztaImpactResponse.Impact.ImpactItem(20.0f, 18.0f, 22.0f, List.of())
                )
            ),
            1
        );

        when(client.getInstanceImpact(eq(true), anyInt(), anySet(), any(BoaviztaInstanceImpactRequest.class)))
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

        KubernetesImpact k8sImpact = (KubernetesImpact) assetImpact.serviceImpacts().getFirst();
        assertEquals(50.0, k8sImpact.loadPercent());
        assertEquals(3, k8sImpact.nodeCount());
    }

    private record TestServerProfile(
        ServerProvider provider,
        String instanceType,
        Country location,
        Lifetime lifespan,
        int nodeCount
    ) implements ServerProfile {}

    private record TestAsset(AssetType type, AssetProfile profile) implements Asset
    {
        @Override
        public AssetIdentity identity()
        {
            return AssetIdentity.of("asset", "env", "Asset");
        }
    }
}
