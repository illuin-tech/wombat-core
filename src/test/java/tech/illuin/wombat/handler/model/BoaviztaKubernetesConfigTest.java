package tech.illuin.wombat.handler.model;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.asset.model.KubernetesAPIAsset;
import tech.illuin.wombat.asset.model.profile.InfrastructureProfile;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactRequest;
import tech.illuin.wombat.kubernetes.KubernetesAPIAssetProperties;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoaviztaKubernetesConfigTest
{

    @Test
    void fromAsset_buildsSingleClusterConfigFromProfileAndCluster()
    {
        InfrastructureProfile profile = new InfrastructureProfile(
            BoaviztaInstanceImpactRequest.Provider.aws, "c5.large", "FRA", 43800
        );
        KubernetesAPIAssetProperties cluster = cluster("c1", "ns-a", profile);

        BoaviztaKubernetesConfig config = BoaviztaKubernetesConfig.fromAsset(new KubernetesAPIAsset("env-1", "Env 1", "c1-name", profile, cluster));

        assertEquals(List.of(new ClusterInfo("c1", "ns-a")), config.clusters());
        assertEquals(BoaviztaInstanceImpactRequest.Provider.aws, config.provider());
        assertEquals("c5.large", config.instanceType());
        assertEquals("FRA", config.location());
        assertEquals(43800, config.lifespan());
    }

    private KubernetesAPIAssetProperties cluster(String id, String namespace, InfrastructureProfile profile)
    {
        return new KubernetesAPIAssetProperties(id, id, "", namespace, Optional.empty(), Optional.empty(), 0, profile);
    }
}
