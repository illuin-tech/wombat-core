package tech.illuin.wombat.environment.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.monitor.AssetType;

import java.time.Duration;

public record AssetCreationRequest(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("type") AssetType type,
    @JsonProperty("profile_id") String profileId,
    @JsonProperty("config_path") String configPath,
    @JsonProperty("namespace") String namespace,
    @JsonProperty("context") String context,
    @JsonProperty("read_timeout") Duration readTimeout
)
{
}
