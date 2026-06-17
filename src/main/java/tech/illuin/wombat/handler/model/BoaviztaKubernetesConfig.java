package tech.illuin.wombat.handler.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.asset.AssetConfig;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactRequest;
import tech.illuin.wombat.model.Datasource;
import tech.illuin.wombat.profile.Profile;

import java.util.List;


public record BoaviztaKubernetesConfig(
    @JsonProperty("provider") BoaviztaInstanceImpactRequest.Provider provider,
    @JsonProperty("instance_type") String instanceType,
    @JsonProperty("location") String location,
    @JsonProperty("lifespan") int lifespan,
    @JsonProperty("clusters") List<ClusterInfo> clusters
) implements ProviderConfig
{

    @Override
    public Datasource datasource()
    {
        return Datasource.KUBERNETES;
    }

    public static BoaviztaKubernetesConfig fromAsset(AssetConfig asset)
    {
        Profile profile = asset.profile();
        ClusterInfo cluster = new ClusterInfo(asset.clusterProperties().id(), asset.clusterProperties().namespace());
        return new BoaviztaKubernetesConfig(
            profile.provider(), profile.instanceType(), profile.location(), profile.lifespan(),
            List.of(cluster)
        );
    }
}
