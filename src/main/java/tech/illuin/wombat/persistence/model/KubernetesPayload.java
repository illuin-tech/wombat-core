package tech.illuin.wombat.persistence.model;

import java.util.Map;

public record KubernetesPayload(
    String clusterId,
    String namespace,
    Map<String, Map<String, String>> pods
) {}
