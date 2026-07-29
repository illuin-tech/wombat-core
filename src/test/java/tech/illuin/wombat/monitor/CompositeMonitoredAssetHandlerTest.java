package tech.illuin.wombat.monitor;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.asset.model.profile.DynamicLLMProfile;
import tech.illuin.wombat.asset.model.profile.InfrastructureProfile;
import tech.illuin.wombat.asset.model.profile.StaticLLMProfile;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactRequest;
import tech.illuin.wombat.ecologits.model.EcologitsEstimationRequest;
import tech.illuin.wombat.kubernetes.KubernetesAPIAssetProperties;
import tech.illuin.wombat.kubernetes.KubernetesMonitorHandler;
import tech.illuin.wombat.llm.LLMMonitorHandler;
import tech.illuin.wombat.llm.LLMPrometheusProperties;
import tech.illuin.wombat.llm.LLMStaticProperties;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompositeMonitoredAssetHandlerTest
{

    private final CompositeMonitoredAssetHandler composite = new CompositeMonitoredAssetHandler(
        List.of(new KubernetesMonitorHandler(null), new LLMMonitorHandler(null)));

    @Test
    void accept_returnsFalseForConfigNoDelegateHandles_withoutClassCast()
    {
        assertFalse(this.composite.accept(new LLMStaticProperties("s", "Static",
            new StaticLLMProfile(EcologitsEstimationRequest.Provider.mistralai, "m", "FRA", new StaticLLMProfile.RequestProfile(500, 1000)))));
    }

    @Test
    void accept_routesEachMonitoredTypeToItsDelegate()
    {
        assertTrue(this.composite.accept(kubernetes(0)));
        assertTrue(this.composite.accept(prometheus(0)));
    }

    @Test
    void heartbeatSkip_isReadFromTheAcceptingDelegate()
    {
        assertEquals(5, this.composite.heartbeatSkip(kubernetes(5)));
        assertEquals(3, this.composite.heartbeatSkip(prometheus(3)));
    }

    private static KubernetesAPIAssetProperties kubernetes(int heartbeatSkip)
    {
        return new KubernetesAPIAssetProperties("k", "K", "/kube/config", "ns", Optional.empty(), Optional.empty(), heartbeatSkip,
            new InfrastructureProfile(BoaviztaInstanceImpactRequest.Provider.aws, "c5.large", "FRA", 43800));
    }

    private static LLMPrometheusProperties prometheus(int heartbeatSkip)
    {
        return new LLMPrometheusProperties("l", "L", "http://prometheus", null, null, null, heartbeatSkip,
            new DynamicLLMProfile(EcologitsEstimationRequest.Provider.mistralai, "m", "FRA", new DynamicLLMProfile.DynamicProfile("q")));
    }
}
