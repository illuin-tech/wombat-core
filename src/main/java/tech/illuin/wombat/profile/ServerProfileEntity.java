package tech.illuin.wombat.profile;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactRequest;

@Entity
@Table(name = "server_profile")
public class ServerProfileEntity extends PanacheEntityBase
{

    @Id
    @Column(nullable = false)
    public String id;

    @Column(nullable = false)
    public String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public BoaviztaInstanceImpactRequest.Provider provider;

    @Column(nullable = false)
    public String instanceType;

    @Column(nullable = false)
    public String location;

    @Column(nullable = false)
    public int lifespan;
}
