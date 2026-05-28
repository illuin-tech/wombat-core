package tech.illuin.vigilantwombat.handler.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.vigilantwombat.boavizta.model.BoaviztaInstanceImpactRequest;
import tech.illuin.vigilantwombat.model.Datasource;
import tech.illuin.vigilantwombat.monitor.MonitorProperties;
import tech.illuin.vigilantwombat.profile.ServerProfileProperties;

import java.util.List;
import java.util.stream.Collectors;

public record BoaviztaKubernetesConfig(
    @JsonProperty("provider") BoaviztaInstanceImpactRequest.Provider provider,
    @JsonProperty("instance_type") String instanceType,
    @JsonProperty("location") String location,
    @JsonProperty("lifespan") int lifespan,
    @JsonProperty("namespaces") List<String> namespaces
) implements ProviderConfig {

    @Override
    public Datasource datasource() {
        return Datasource.KUBERNETES;
    }

    public static BoaviztaKubernetesConfig fromServerProfile(ServerProfileProperties serverProps, MonitorProperties monitorProps) {
        List<String> namespaces = monitorProps.kubernetesConfigs().values().stream()
            .map(c -> c.namespace())
            .collect(Collectors.toList());
        return new BoaviztaKubernetesConfig(
            serverProps.provider(), serverProps.instanceType(), serverProps.location(), serverProps.lifespan(),
            namespaces
        );
    }
}