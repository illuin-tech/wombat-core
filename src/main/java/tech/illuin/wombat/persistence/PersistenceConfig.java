package tech.illuin.wombat.persistence;

import io.agroal.api.AgroalDataSource;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import org.flywaydb.core.Flyway;
import tech.illuin.wombat.metrics.MetricRecorderService;
import tech.illuin.wombat.persistence.backup.BackupProperties;
import tech.illuin.wombat.persistence.backup.SqliteBackupService;
import tech.illuin.wombat.persistence.observability.SQLiteSizeGauge;

import java.io.File;

@ApplicationScoped
public class PersistenceConfig
{

    static final int STARTUP_PRIORITY_BOOTSTRAP = 1000;

    private SQLiteSizeGauge sizeGauge;

    void onStart(
        @Observes @Priority(STARTUP_PRIORITY_BOOTSTRAP) StartupEvent event,
        Flyway flyway,
        AgroalDataSource dataSource,
        MeterRegistry registry
    )
    {
        new File("data/db").mkdirs();
        flyway.migrate();

        this.sizeGauge = new SQLiteSizeGauge(dataSource);
        Gauge.builder("sqlite.db.size.bytes", this.sizeGauge, SQLiteSizeGauge::sizeBytes)
            .description("SQLite database size in bytes")
            .register(registry);
    }

    @Singleton
    @IfBuildProperty(name = "persistence.enable-kubernetes-metrics-persister", stringValue = "true")
    public KubernetesMetricsPersister provideKubernetesMetricsPersister(ServerMetricRepository repository, MetricRecorderService recorder)
    {
        return new KubernetesMetricsPersister(repository, recorder);
    }

    @Singleton
    @IfBuildProperty(name = "backup.enabled", stringValue = "true")
    public SqliteBackupService provideSqliteBackupService(AgroalDataSource dataSource, BackupProperties props)
    {
        return new SqliteBackupService(dataSource, props);
    }
}
