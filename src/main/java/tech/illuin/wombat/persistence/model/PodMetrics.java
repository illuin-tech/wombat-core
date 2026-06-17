package tech.illuin.wombat.persistence.model;

import java.util.Map;

public record PodMetrics(Map<String, ContainerMetrics> pods)
{
    public record ContainerMetrics(Map<String, String> containers) {}
}
