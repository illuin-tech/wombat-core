package tech.illuin.wombat.handler.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.asset.model.KubernetesAsset;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactRequest;
import tech.illuin.wombat.monitor.AssetType;
import tech.illuin.wombat.profile.model.InfrastructureProfile;

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
    public AssetType datasource()
    {
        return AssetType.KUBERNETES_API;
    }

    public static BoaviztaKubernetesConfig fromAsset(KubernetesAsset asset)
    {
        InfrastructureProfile profile = asset.profile();
        ClusterInfo cluster = new ClusterInfo(asset.properties().id(), asset.properties().namespace());
        return new BoaviztaKubernetesConfig(
            profile.provider(), profile.instanceType(), profile.location(), profile.lifespan(),
            List.of(cluster)
        );
    }
}
