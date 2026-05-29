package tech.illuin.wombat.persistence.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "datapoint")
public class DatapointEntity extends PanacheEntityBase
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INTEGER")
    public Long id;

    @Column(nullable = false)
    public long instantMs;

    @Column(nullable = false)
    public String type;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String payload;
}
