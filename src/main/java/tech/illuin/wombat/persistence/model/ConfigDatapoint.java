package tech.illuin.wombat.persistence.model;

public sealed interface ConfigDatapoint permits KubernetesConfigDatapoint
{
    ConfigType configType();
}
