package tech.illuin.wombat.persistence.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "kubernetes_metrics")
public class KubernetesMetricEntity extends PanacheEntityBase
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INTEGER")
    public Long id;

    @Column(nullable = false, columnDefinition = "INTEGER")
    public long instantMs;

    @Column
    public String cluster;

    @Column
    public String namespace;

    @Column(nullable = false)
    public String pod;

    @Column(nullable = false)
    public String container;

    @Column(name = "cpu_nanocores", nullable = false, columnDefinition = "REAL")
    public double cpuNanocores;

    @Column(name = "ram_bytes", nullable = false, columnDefinition = "REAL")
    public double ramBytes;
}
