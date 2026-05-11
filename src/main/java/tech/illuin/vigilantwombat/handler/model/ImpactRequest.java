package tech.illuin.vigilantwombat.handler.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.vigilantwombat.persistence.model.TimeRange;
import tech.illuin.vigilantwombat.profile.ServerConfig;

public record ImpactRequest(
    @JsonProperty("server_config") ServerConfig serverConfig,
    @JsonProperty("source_time_range") TimeRange sourceTimeRange
) {}
