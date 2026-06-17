package tech.illuin.wombat.asset;

import tech.illuin.wombat.k8s.ClusterProperties;
import tech.illuin.wombat.profile.Profile;

public record AssetConfig(
    String name,
    Profile profile,
    ClusterProperties clusterProperties
) {}
