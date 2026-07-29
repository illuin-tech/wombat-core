package tech.illuin.wombat.asset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tech.illuin.wombat.asset.model.Asset;
import tech.illuin.wombat.asset.model.profile.InfrastructureProfile;
import tech.illuin.wombat.asset.model.profile.StaticLLMProfile;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactRequest;
import tech.illuin.wombat.ecologits.model.EcologitsEstimationRequest;
import tech.illuin.wombat.environment.ActiveEnvironments;
import tech.illuin.wombat.environment.persistence.AssetEntity;
import tech.illuin.wombat.environment.persistence.AssetRepository;
import tech.illuin.wombat.environment.persistence.EnvironmentEntity;
import tech.illuin.wombat.environment.persistence.EnvironmentRepository;
import tech.illuin.wombat.kubernetes.KubernetesAPIAssetProperties;
import tech.illuin.wombat.llm.LLMStaticProperties;
import tech.illuin.wombat.monitor.AssetType;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class AssetServiceTest
{

    private AssetService assetService;
    private EnvironmentRepository environmentRepository;

    @BeforeEach
    void setUp()
    {
        this.environmentRepository = Mockito.mock(EnvironmentRepository.class);
        when(this.environmentRepository.findActive()).thenReturn(List.of(activeEnvironment("env-a", "Env A"), activeEnvironment("env-b", "Env B")));
        AssetRepository assetRepository = Mockito.mock(AssetRepository.class);
        when(assetRepository.findByEnvironment("env-a")).thenReturn(List.of(
            AssetEntity.from("env-a", new KubernetesAPIAssetProperties("cluster-a", "Cluster A", "/kube/config", "ns", Optional.empty(), Optional.empty(), 0, infrastructureProfile()))
        ));
        when(assetRepository.findByEnvironment("env-b")).thenReturn(List.of(
            AssetEntity.from("env-b", new LLMStaticProperties("llm-b", "LLM B", llmProfile()))
        ));
        this.assetService = new AssetService(new ActiveEnvironments(this.environmentRepository, assetRepository));
    }

    @Test
    void allBuildsEveryAssetFromItsInlineProfile()
    {
        List<Asset> assets = this.assetService.all();

        assertEquals(2, assets.size());
        assertTrue(assets.stream().anyMatch(a -> a.properties().id().equals("cluster-a")));
        assertTrue(assets.stream().anyMatch(a -> a.properties().id().equals("llm-b")));
    }

    @Test
    void allExcludesDisabledEnvironments()
    {
        when(this.environmentRepository.findActive()).thenReturn(List.of(activeEnvironment("env-a", "Env A")));

        List<Asset> assets = this.assetService.all();

        assertEquals(1, assets.size());
        assertEquals("cluster-a", assets.getFirst().properties().id());
        assertTrue(this.assetService.resolve("env-b", List.of()).isEmpty());
    }

    @Test
    void resolveWithoutFiltersReturnsEveryResolvableAsset()
    {
        List<Asset> assets = this.assetService.resolve(null, List.of());

        assertEquals(2, assets.size());
    }

    @Test
    void resolveFiltersByEnvironment()
    {
        List<Asset> assets = this.assetService.resolve("env-a", List.of());

        assertEquals(1, assets.size());
        assertEquals("cluster-a", assets.getFirst().properties().id());
        assertEquals(AssetType.KUBERNETES_API, assets.getFirst().type());
    }

    @Test
    void resolveFiltersByAssetIds()
    {
        List<Asset> assets = this.assetService.resolve(null, List.of("llm-b"));

        assertEquals(1, assets.size());
        assertEquals(AssetType.LLM_STATIC, assets.getFirst().type());
    }

    @Test
    void resolveUnknownEnvironmentReturnsEmpty()
    {
        assertTrue(this.assetService.resolve("unknown", List.of()).isEmpty());
    }

    private static EnvironmentEntity activeEnvironment(String id, String name)
    {
        EnvironmentEntity entity = new EnvironmentEntity();
        entity.id = id;
        entity.name = name;
        return entity;
    }

    private static InfrastructureProfile infrastructureProfile()
    {
        return new InfrastructureProfile(BoaviztaInstanceImpactRequest.Provider.aws, "c5.large", "FRA", 43800);
    }

    private static StaticLLMProfile llmProfile()
    {
        return new StaticLLMProfile(
            EcologitsEstimationRequest.Provider.mistralai, "mistral-large-latest", "FRA",
            new StaticLLMProfile.RequestProfile(500, 1000)
        );
    }
}
