package tech.illuin.vigilantwombat.handler.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.vigilantwombat.model.Footprint;
import tech.illuin.vigilantwombat.persistence.model.TimeRange;

import java.util.List;
import java.util.Map;

public record ImpactResponse(
    @JsonProperty("global_impact") Footprint globalImpact,
    @JsonProperty("service_impacts") List<Footprint> serviceImpacts,
    @JsonProperty("impact_shares") Map<String, Double> impactShares,
    @JsonProperty("provided_input") ImpactRequest providedInput,
    @JsonProperty("parameters") Parameters parameters,
    @JsonProperty("containers") List<String> containers
) {
    public record Parameters(
        @JsonProperty("provider_config") ProviderConfig providerConfig,
        @JsonProperty("time_range") TimeRange timeRange
    ) {}
}
