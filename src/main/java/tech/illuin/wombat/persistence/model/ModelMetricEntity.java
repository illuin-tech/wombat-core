package tech.illuin.wombat.persistence.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "model_metrics")
public class ModelMetricEntity extends PanacheEntityBase
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INTEGER")
    public Long id;

    @Column(nullable = false, columnDefinition = "INTEGER")
    public long instantMs;

    @Convert(converter = MetricDataConverter.class)
    @Column(nullable = false)
    public MetricData data;

    @Column(nullable = false, columnDefinition = "INTEGER")
    public long outputTokens;
}
