package tech.illuin.wombat.handler.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.ui.AssetImpact;

import java.util.Map;

public record ImpactResponse(
    @JsonProperty("global") GlobalImpact global,
    @JsonProperty("asset_impacts") Map<String, AssetImpact> assetImpacts,
    @JsonProperty("config") EnvironmentConfig config
) {}
