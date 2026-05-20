package tech.illuin.vigilantwombat.persistence.model;

public sealed interface ConfigDatapoint permits KubernetesConfigDatapoint {
    ConfigType configType();
}