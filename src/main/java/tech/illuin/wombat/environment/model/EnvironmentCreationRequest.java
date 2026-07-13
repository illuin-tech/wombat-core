package tech.illuin.wombat.environment.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EnvironmentCreationRequest(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name
)
{
}
