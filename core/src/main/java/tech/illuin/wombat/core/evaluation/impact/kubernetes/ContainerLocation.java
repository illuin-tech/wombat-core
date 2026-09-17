package tech.illuin.wombat.core.evaluation.impact.kubernetes;

public record ContainerLocation(
    String clusterId,
    String namespace
) {}
