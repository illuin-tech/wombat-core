package tech.illuin.wombat.environment.persistence;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.asset.model.profile.DynamicLLMProfile;
import tech.illuin.wombat.asset.model.profile.InfrastructureProfile;
import tech.illuin.wombat.asset.model.profile.StaticLLMProfile;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactRequest;
import tech.illuin.wombat.ecologits.model.EcologitsEstimationRequest;
import tech.illuin.wombat.kubernetes.KubernetesAPIAssetProperties;
import tech.illuin.wombat.llm.LLMPrometheusProperties;
import tech.illuin.wombat.llm.LLMStaticProperties;
import tech.illuin.wombat.monitor.AssetType;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssetEntityTest
{

    @Test
    void kubernetesPropertiesRoundTripThroughTheEntity()
    {
        KubernetesAPIAssetProperties props = new KubernetesAPIAssetProperties(
            "k", "K", "/kube/config", "ns", Optional.of("ctx"), Optional.of(Duration.ofSeconds(5)), 3,
            new InfrastructureProfile(BoaviztaInstanceImpactRequest.Provider.aws, "c5.large", "FRA", 43800));

        AssetEntity entity = AssetEntity.from("env", props);

        assertEquals("env", entity.environmentId);
        assertEquals(AssetType.KUBERNETES_API, entity.type);
        assertEquals(props, entity.toProperties());
    }

    @Test
    void llmStaticPropertiesRoundTripThroughTheEntity()
    {
        LLMStaticProperties props = new LLMStaticProperties("s", "S",
            new StaticLLMProfile(EcologitsEstimationRequest.Provider.mistralai, "m", "FRA",
                new StaticLLMProfile.RequestProfile(500, 1000)));

        AssetEntity entity = AssetEntity.from("env", props);

        assertEquals(AssetType.LLM_STATIC, entity.type);
        assertEquals(props, entity.toProperties());
    }

    @Test
    void llmPrometheusPropertiesRoundTripThroughTheEntity()
    {
        LLMPrometheusProperties props = new LLMPrometheusProperties("p", "P",
            "http://prometheus", "http://proxy:8080", "user", "secret", 7,
            new DynamicLLMProfile(EcologitsEstimationRequest.Provider.mistralai, "m", "FRA",
                new DynamicLLMProfile.DynamicProfile("q")));

        AssetEntity entity = AssetEntity.from("env", props);

        assertEquals(AssetType.LLM_PROMETHEUS, entity.type);
        assertEquals(props, entity.toProperties());
    }
}
