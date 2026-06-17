package tech.illuin.wombat.handler.model;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.asset.AssetConfig;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactRequest;
import tech.illuin.wombat.k8s.ClusterProperties;
import tech.illuin.wombat.profile.Profile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoaviztaKubernetesConfigTest
{

    @Test
    void fromAsset_buildsSingleClusterConfigFromProfileAndCluster()
    {
        Profile profile = new Profile(
            "aws-c5", "AWS c5.large", BoaviztaInstanceImpactRequest.Provider.aws,
            "c5.large", "FRA", 43800
        );
        ClusterProperties cluster = cluster("c1", "ns-a", "aws-c5");

        BoaviztaKubernetesConfig config = BoaviztaKubernetesConfig.fromAsset(new AssetConfig("c1-name", profile, cluster));

        assertEquals(List.of(new ClusterInfo("c1", "ns-a")), config.clusters());
        assertEquals(BoaviztaInstanceImpactRequest.Provider.aws, config.provider());
        assertEquals("c5.large", config.instanceType());
        assertEquals("FRA", config.location());
        assertEquals(43800, config.lifespan());
    }

    private ClusterProperties cluster(String id, String namespace, String profileId)
    {
        return new ClusterProperties(id, id, profileId, "", namespace, Optional.empty(), Optional.empty());
    }
}
