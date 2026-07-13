package tech.illuin.wombat.handler.model;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.asset.model.KubernetesAsset;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactRequest;
import tech.illuin.wombat.kubernetes.KubernetesAssetProperties;
import tech.illuin.wombat.profile.model.InfrastructureProfile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoaviztaKubernetesConfigTest
{

    @Test
    void fromAsset_buildsSingleClusterConfigFromProfileAndCluster()
    {
        InfrastructureProfile profile = new InfrastructureProfile(
            "aws-c5", "AWS c5.large", BoaviztaInstanceImpactRequest.Provider.aws,
            "c5.large", "FRA", 43800
        );
        KubernetesAssetProperties cluster = cluster("c1", "ns-a", "aws-c5");

        BoaviztaKubernetesConfig config = BoaviztaKubernetesConfig.fromAsset(new KubernetesAsset("env-1", "Env 1", "c1-name", profile, cluster));

        assertEquals(List.of(new ClusterInfo("c1", "ns-a")), config.clusters());
        assertEquals(BoaviztaInstanceImpactRequest.Provider.aws, config.provider());
        assertEquals("c5.large", config.instanceType());
        assertEquals("FRA", config.location());
        assertEquals(43800, config.lifespan());
    }

    private KubernetesAssetProperties cluster(String id, String namespace, String profileId)
    {
        return new KubernetesAssetProperties(id, id, profileId, "", namespace, Optional.empty(), Optional.empty());
    }
}
