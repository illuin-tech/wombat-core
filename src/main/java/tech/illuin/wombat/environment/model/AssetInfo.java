package tech.illuin.wombat.environment.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.environment.persistence.AssetData;
import tech.illuin.wombat.environment.persistence.AssetEntity;
import tech.illuin.wombat.monitor.AssetType;

import java.time.Duration;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AssetInfo(
    @JsonProperty("id") String id,
    @JsonProperty("uuid") String uuid,
    @JsonProperty("name") String name,
    @JsonProperty("type") AssetType type,
    @JsonProperty("config_path") String configPath,
    @JsonProperty("namespace") String namespace,
    @JsonProperty("context") String context,
    @JsonProperty("read_timeout") Duration readTimeout,
    @JsonProperty("prometheus_url") String prometheusUrl,
    @JsonProperty("proxy_url") String proxyUrl,
    @JsonProperty("username") String username,
    @JsonProperty("heartbeat_skip") Integer heartbeatSkip,
    @JsonProperty("profile") Object profile,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("updated_at") Instant updatedAt,
    @JsonProperty("deleted_at") Instant deletedAt
)
{
    // The Prometheus password is intentionally never exposed in the REST representation.
    public static AssetInfo from(AssetEntity entity)
    {
        return switch (entity.data)
        {
            case AssetData.KubernetesAPIData kubernetes -> new AssetInfo(
                entity.id, entity.uuid, entity.name, entity.type,
                kubernetes.configPath(), kubernetes.namespace(), kubernetes.context(), kubernetes.readTimeout(),
                null, null, null, kubernetes.heartbeatSkip(),
                kubernetes.profile(),
                entity.createdAt, entity.updatedAt, entity.deletedAt
            );
            case AssetData.LLMStaticData llm -> new AssetInfo(
                entity.id, entity.uuid, entity.name, entity.type,
                null, null, null, null,
                null, null, null, null,
                llm.profile(),
                entity.createdAt, entity.updatedAt, entity.deletedAt
            );
            case AssetData.LLMPrometheusData prometheus -> new AssetInfo(
                entity.id, entity.uuid, entity.name, entity.type,
                null, null, null, null,
                prometheus.prometheusUrl(), prometheus.proxyUrl(), prometheus.username(), prometheus.heartbeatSkip(),
                prometheus.profile(),
                entity.createdAt, entity.updatedAt, entity.deletedAt
            );
        };
    }
}
