package tech.illuin.wombat.persistence.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "datapoint", indexes = @Index(columnList = "instantMs, type"))
public class DatapointEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public long instantMs;

    @Column(nullable = false)
    public String type;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String payload;
}
