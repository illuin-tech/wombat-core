package tech.illuin.vigilantwombat.persistence.model;

import io.fabric8.kubernetes.api.model.metrics.v1beta1.ContainerMetrics;

import java.util.List;
import java.util.Map;

public record KubernetesConfigDatapoint(Map<String, List<ContainerMetrics>> podMetrics) implements ConfigDatapoint {

    @Override
    public ConfigType configType() {
        return ConfigType.KUBERNETES;
    }
}