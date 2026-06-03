package tech.illuin.wombat.handler.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactRequest;
import tech.illuin.wombat.model.Datasource;
import tech.illuin.wombat.monitor.MonitorProperties;
import tech.illuin.wombat.profile.ServerProfileEntity;

import java.util.List;
import java.util.stream.Collectors;


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

    public static BoaviztaKubernetesConfig fromProfileEntity(
        ServerProfileEntity profile,
        MonitorProperties monitorProperties,
        List<String> selectedClusterIds
    )
    {
        List<ClusterInfo> clusters = monitorProperties.k8sConfigs().clusters().stream()
            .filter(c -> selectedClusterIds.isEmpty() || selectedClusterIds.contains(c.id()))
            .map(c -> new ClusterInfo(c.id(), c.namespace()))
            .collect(Collectors.toList());
        return new BoaviztaKubernetesConfig(
            profile.provider, profile.instanceType, profile.location, profile.lifespan,
            clusters
        );
    }
}
