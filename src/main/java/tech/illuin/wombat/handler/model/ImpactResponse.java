package tech.illuin.wombat.handler.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.model.Footprint;
import tech.illuin.wombat.persistence.model.TimeRange;

import java.util.List;
import java.util.Map;

public record ImpactResponse(
    @JsonProperty("global_impact") Footprint globalImpact,
    @JsonProperty("service_impacts") List<Footprint> serviceImpacts,
    @JsonProperty("impact_shares") Map<String, Double> impactShares,
    @JsonProperty("provided_input") ImpactRequest providedInput,
    @JsonProperty("parameters") Parameters parameters,
    @JsonProperty("containers") List<String> containers,
    @JsonProperty("container_locations") Map<String, List<ClusterInfo>> containerLocations,
    @JsonProperty("cpu_usage_cores") double cpuUsageCores
) {
    public record Parameters(
        @JsonProperty("provider_config") ProviderConfig providerConfig,
        @JsonProperty("time_range") TimeRange timeRange
    ) {}
}
