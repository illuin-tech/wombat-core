package tech.illuin.wombat.environment.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import tech.illuin.wombat.kubernetes.KubernetesAPIAssetProperties;
import tech.illuin.wombat.llm.LLMPrometheusProperties;
import tech.illuin.wombat.llm.LLMStaticProperties;
import tech.illuin.wombat.monitor.AssetProperties;
import tech.illuin.wombat.monitor.AssetType;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;


@Entity
@Table(name = "assets")
public class AssetEntity extends PanacheEntityBase
{

    @Id
    @Column(nullable = false)
    public String id;

    @Column(nullable = false, unique = true)
    public String uuid;

    @Column(name = "environment_id", nullable = false)
    public String environmentId;

    @Column(nullable = false)
    public String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public AssetType type;

    @Convert(converter = AssetDataConverter.class)
    @Column(nullable = false)
    public AssetData data;

    @Column(name = "created_at", nullable = false, columnDefinition = "INTEGER")
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "INTEGER")
    public Instant updatedAt;

    @Column(name = "deleted_at", columnDefinition = "INTEGER")
    public Instant deletedAt;

    @PrePersist
    void onPersist()
    {
        if (this.uuid == null)
            this.uuid = UUID.randomUUID().toString();
        this.createdAt = EnvironmentEntity.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate()
    {
        this.updatedAt = EnvironmentEntity.now();
    }

    public AssetProperties toProperties()
    {
        return switch (this.data)
        {
            case AssetData.KubernetesAPIData kubernetes -> new KubernetesAPIAssetProperties(
                this.id, this.name,
                kubernetes.configPath(), kubernetes.namespace(),
                Optional.ofNullable(kubernetes.context()), Optional.ofNullable(kubernetes.readTimeout()),
                kubernetes.heartbeatSkip(), kubernetes.profile()
            );
            case AssetData.LLMStaticData llm -> new LLMStaticProperties(this.id, this.name, llm.profile());
            case AssetData.LLMPrometheusData prometheus -> new LLMPrometheusProperties(
                this.id, this.name,
                prometheus.prometheusUrl(), prometheus.proxyUrl(), prometheus.username(), prometheus.password(),
                prometheus.heartbeatSkip(), prometheus.profile()
            );
        };
    }

    public static AssetEntity from(String environmentId, AssetProperties properties)
    {
        AssetEntity entity = new AssetEntity();
        entity.id = properties.id();
        entity.environmentId = environmentId;
        entity.name = properties.name();
        entity.type = properties.type();
        entity.data = dataFrom(properties);
        return entity;
    }

    public static AssetData dataFrom(AssetProperties properties)
    {
        return switch (properties)
        {
            case KubernetesAPIAssetProperties kubernetes -> new AssetData.KubernetesAPIData(
                kubernetes.configPath(), kubernetes.namespace(),
                kubernetes.context().orElse(null), kubernetes.readTimeout().orElse(null),
                kubernetes.heartbeatSkip(), kubernetes.profile()
            );
            case LLMStaticProperties llm -> new AssetData.LLMStaticData(llm.profile());
            case LLMPrometheusProperties llm -> new AssetData.LLMPrometheusData(
                llm.prometheusUrl(), llm.proxyUrl(), llm.username(), llm.password(), llm.heartbeatSkip(), llm.profile()
            );
            default -> throw new IllegalArgumentException("Unsupported asset type: " + properties.type());
        };
    }
}
