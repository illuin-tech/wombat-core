package tech.illuin.wombat.profile.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import tech.illuin.wombat.profile.model.ProfileType;

import java.util.UUID;

@Entity
@Table(name = "profiles")
public class ProfileEntity extends PanacheEntityBase
{

    @Id
    @Column(nullable = false)
    public String id;

    @Column(nullable = false, unique = true)
    public String uuid;

    @Column(nullable = false)
    public String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public ProfileType type;

    @Column(nullable = false)
    public String provider;

    @Column(nullable = false)
    public String location;

    @Convert(converter = ProfileDataConverter.class)
    @Column(nullable = false)
    public ProfileData data;

    @PrePersist
    void onPersist()
    {
        if (this.uuid == null)
            this.uuid = UUID.randomUUID().toString();
    }
}
