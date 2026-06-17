package tech.illuin.wombat.asset;

import tech.illuin.wombat.k8s.K8SProperties;
import tech.illuin.wombat.profile.Profile;

public record AssetConfig(
    String name,
    Profile profile,
    K8SProperties.ClusterProperties clusterProperties
) {}
