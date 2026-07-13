package tech.illuin.wombat.environment;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.environment.persistence.AssetData;
import tech.illuin.wombat.environment.persistence.AssetEntity;
import tech.illuin.wombat.environment.persistence.AssetRepository;
import tech.illuin.wombat.environment.persistence.EnvironmentEntity;
import tech.illuin.wombat.environment.persistence.EnvironmentRepository;
import tech.illuin.wombat.monitor.AssetType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class EnvironmentSeederTest
{

    @Inject
    EnvironmentRepository repository;

    @Inject
    AssetRepository assetRepository;

    @Inject
    EnvironmentSeeder seeder;

    @Test
    void environmentsFromConfigAreInsertedActiveAtStartup()
    {
        EnvironmentEntity seeded = this.repository.findByIdOptional("test").orElse(null);

        assertNotNull(seeded);
        assertNotNull(seeded.uuid);
        assertEquals("Test", seeded.name);
        assertNotNull(seeded.createdAt);
        assertNull(seeded.disabledAt);
    }

    @Test
    void assetsFromConfigAreInsertedAtStartup()
    {
        AssetEntity cluster = this.assetRepository.findByIdOptional("test-cluster").orElse(null);
        assertNotNull(cluster);
        assertNotNull(cluster.uuid);
        assertEquals("test", cluster.environmentId);
        assertEquals("Test Cluster", cluster.name);
        assertEquals(AssetType.KUBERNETES_API, cluster.type);
        assertEquals("test-aws-c5", cluster.profileId);
        assertEquals(new AssetData.KubernetesData("src/test/resources/test-kubeconfig.yaml", "test-ns", null, null), cluster.data);

        AssetEntity llm = this.assetRepository.findByIdOptional("test-llm").orElse(null);
        assertNotNull(llm);
        assertEquals("test", llm.environmentId);
        assertEquals(AssetType.LLM_STATIC, llm.type);
        assertEquals("test-mistral-large", llm.profileId);
        assertEquals(new AssetData.LLMData(), llm.data);
    }

    @Test
    @Transactional
    void onStart_addsMissingEnvironmentsWithoutDuplicatingExistingOnes()
    {
        long initial = this.repository.count();
        this.repository.deleteById("test");
        assertEquals(initial - 1, this.repository.count());

        this.seeder.onStart(null);

        assertEquals(initial, this.repository.count(), "missing environment re-added, present ones not duplicated");
        assertTrue(this.repository.findByIdOptional("test").isPresent());
    }

    @Test
    @Transactional
    void onStart_addsMissingAssetsWithoutDuplicatingExistingOnes()
    {
        long initial = this.assetRepository.count();
        this.assetRepository.deleteById("test-llm");
        assertEquals(initial - 1, this.assetRepository.count());

        this.seeder.onStart(null);

        assertEquals(initial, this.assetRepository.count(), "missing asset re-added, present ones not duplicated");
        assertTrue(this.assetRepository.findByIdOptional("test-llm").isPresent());
    }
}
