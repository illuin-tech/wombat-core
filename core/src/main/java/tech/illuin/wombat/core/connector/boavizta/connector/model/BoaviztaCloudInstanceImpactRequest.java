package tech.illuin.wombat.core.connector.boavizta.connector.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BoaviztaCloudInstanceImpactRequest(
    @JsonProperty("provider") BoaviztaServerProvider provider,
    @JsonProperty("instance_type") String instanceType,
    @JsonProperty("usage") BoaviztaUsage usage
) {}
