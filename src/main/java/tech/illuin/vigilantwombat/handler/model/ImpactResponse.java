package tech.illuin.vigilantwombat.handler.model;

import tech.illuin.vigilantwombat.boavizta.model.BoaviztaInstanceImpactResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.vigilantwombat.persistence.model.TimeRange;
import tech.illuin.vigilantwombat.profile.ServerConfig;

import java.util.Map;

public record ImpactResponse(
    @JsonProperty("global_impact") BoaviztaInstanceImpactResponse globalImpact,
    @JsonProperty("service_impact") Map<String, BoaviztaInstanceImpactResponse> serviceImpact,
    @JsonProperty("impact_shares") Map<String, Double> impactShares,
    @JsonProperty("provided_input") ImpactRequest providedInput,
    @JsonProperty("parameters") Parameters parameters
) {
    public record Parameters(
        @JsonProperty("server_config") ServerConfig serverConfig,
        @JsonProperty("time_range") TimeRange timeRange
    ) {}
}
