package tech.illuin.wombat.environment.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import tech.illuin.wombat.kubernetes.KubernetesAssetProperties;
import tech.illuin.wombat.llm.LLMProperties;
import tech.illuin.wombat.monitor.AssetProperties;
import tech.illuin.wombat.monitor.AssetType;

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

    @Column(name = "profile_id", nullable = false)
    public String profileId;

    @Convert(converter = AssetDataConverter.class)
    @Column(nullable = false)
    public AssetData data;

    @PrePersist
    void onPersist()
    {
        if (this.uuid == null)
            this.uuid = UUID.randomUUID().toString();
    }

    public AssetProperties toProperties()
    {
        return switch (this.data)
        {
            case AssetData.KubernetesData kubernetes -> new KubernetesAssetProperties(
                this.id, this.name, this.profileId,
                kubernetes.configPath(), kubernetes.namespace(),
                Optional.ofNullable(kubernetes.context()), Optional.ofNullable(kubernetes.readTimeout())
            );
            case AssetData.LLMData ignored -> new LLMProperties(this.id, this.name, this.profileId);
        };
    }

    public static AssetEntity from(String environmentId, AssetProperties properties)
    {
        AssetEntity entity = new AssetEntity();
        entity.id = properties.id();
        entity.environmentId = environmentId;
        entity.name = properties.name();
        entity.type = properties.type();
        switch (properties)
        {
            case KubernetesAssetProperties kubernetes -> {
                entity.profileId = kubernetes.profileId();
                entity.data = new AssetData.KubernetesData(
                    kubernetes.configPath(), kubernetes.namespace(),
                    kubernetes.context().orElse(null), kubernetes.readTimeout().orElse(null)
                );
            }
            case LLMProperties llm -> {
                entity.profileId = llm.profileId();
                entity.data = new AssetData.LLMData();
            }
            default -> throw new IllegalArgumentException("Unsupported asset type: " + properties.type());
        }
        return entity;
    }
}
