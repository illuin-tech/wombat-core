package tech.illuin.wombat.handler.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.model.Footprint;

import java.util.List;
import java.util.Map;

public record KubernetesImpactResponse(
    @JsonProperty("global_impact") Footprint globalImpact,
    @JsonProperty("service_impacts") List<Footprint> serviceImpacts,
    @JsonProperty("impact_shares") Map<String, Double> impactShares,
    @JsonProperty("provider_config") ProviderConfig providerConfig,
    @JsonProperty("services") List<String> services,
    @JsonProperty("service_locations") Map<String, List<ClusterInfo>> serviceLocations,
    @JsonProperty("cpu_usage_cores") double cpuUsageCores
) {}
