package tech.illuin.wombat.asset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tech.illuin.wombat.asset.model.Asset;
import tech.illuin.wombat.environment.ActiveEnvironments;
import tech.illuin.wombat.environment.persistence.AssetEntity;
import tech.illuin.wombat.environment.persistence.AssetRepository;
import tech.illuin.wombat.environment.persistence.EnvironmentEntity;
import tech.illuin.wombat.environment.persistence.EnvironmentRepository;
import tech.illuin.wombat.kubernetes.KubernetesAssetProperties;
import tech.illuin.wombat.llm.LLMProperties;
import tech.illuin.wombat.monitor.AssetType;
import tech.illuin.wombat.profile.model.ProfileType;
import tech.illuin.wombat.profile.persistence.ProfileData;
import tech.illuin.wombat.profile.persistence.ProfileEntity;
import tech.illuin.wombat.profile.persistence.ProfileRepository;

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
        ProfileRepository repository = Mockito.mock(ProfileRepository.class);
        when(repository.findByIdOptional("p-infra")).thenReturn(Optional.of(infrastructureProfile()));
        when(repository.findByIdOptional("p-llm")).thenReturn(Optional.of(llmProfile()));
        when(repository.findByIdOptional("p-missing")).thenReturn(Optional.empty());
        this.environmentRepository = Mockito.mock(EnvironmentRepository.class);
        when(this.environmentRepository.findActive()).thenReturn(List.of(activeEnvironment("env-a", "Env A"), activeEnvironment("env-b", "Env B")));
        AssetRepository assetRepository = Mockito.mock(AssetRepository.class);
        when(assetRepository.findByEnvironment("env-a")).thenReturn(List.of(
            AssetEntity.from("env-a", new KubernetesAssetProperties("cluster-a", "Cluster A", "p-infra", "/kube/config", "ns", Optional.empty(), Optional.empty())),
            AssetEntity.from("env-a", new KubernetesAssetProperties("cluster-broken", "Cluster Broken", "p-missing", "/kube/config", "ns", Optional.empty(), Optional.empty()))
        ));
        when(assetRepository.findByEnvironment("env-b")).thenReturn(List.of(
            AssetEntity.from("env-b", new LLMProperties("llm-b", "LLM B", "p-llm"))
        ));
        this.assetService = new AssetService(new ActiveEnvironments(this.environmentRepository, assetRepository), repository);
    }

    @Test
    void allSkipsAssetsWhoseProfileIsMissing()
    {
        List<Asset> assets = this.assetService.all();

        assertEquals(2, assets.size());
        assertTrue(assets.stream().noneMatch(a -> a.properties().id().equals("cluster-broken")));
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

    private static ProfileEntity infrastructureProfile()
    {
        ProfileEntity entity = new ProfileEntity();
        entity.id = "p-infra";
        entity.description = "Infra profile";
        entity.type = ProfileType.INFRASTRUCTURE;
        entity.provider = "aws";
        entity.location = "FRA";
        entity.data = new ProfileData.InfrastructureData("c5.large", 43800);
        return entity;
    }

    private static ProfileEntity llmProfile()
    {
        ProfileEntity entity = new ProfileEntity();
        entity.id = "p-llm";
        entity.description = "LLM profile";
        entity.type = ProfileType.LLM;
        entity.provider = "mistralai";
        entity.location = "FRA";
        entity.data = new ProfileData.LLMData("mistral-large-latest", 500, 1000);
        return entity;
    }
}
