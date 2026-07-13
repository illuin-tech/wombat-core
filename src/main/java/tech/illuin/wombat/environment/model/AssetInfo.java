package tech.illuin.wombat.environment.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.environment.persistence.AssetData;
import tech.illuin.wombat.environment.persistence.AssetEntity;
import tech.illuin.wombat.monitor.AssetType;

import java.time.Duration;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AssetInfo(
    @JsonProperty("id") String id,
    @JsonProperty("uuid") String uuid,
    @JsonProperty("name") String name,
    @JsonProperty("type") AssetType type,
    @JsonProperty("profile_id") String profileId,
    @JsonProperty("config_path") String configPath,
    @JsonProperty("namespace") String namespace,
    @JsonProperty("context") String context,
    @JsonProperty("read_timeout") Duration readTimeout
)
{
    public static AssetInfo from(AssetEntity entity)
    {
        return switch (entity.data)
        {
            case AssetData.KubernetesData kubernetes -> new AssetInfo(
                entity.id, entity.uuid, entity.name, entity.type, entity.profileId,
                kubernetes.configPath(), kubernetes.namespace(), kubernetes.context(), kubernetes.readTimeout()
            );
            case AssetData.LLMData ignored -> new AssetInfo(
                entity.id, entity.uuid, entity.name, entity.type, entity.profileId,
                null, null, null, null
            );
        };
    }
}
