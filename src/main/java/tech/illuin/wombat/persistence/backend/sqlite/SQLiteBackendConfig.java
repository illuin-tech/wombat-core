package tech.illuin.wombat.persistence.backend.sqlite;

import io.agroal.api.AgroalDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.flywaydb.core.Flyway;
import tech.illuin.wombat.persistence.backend.PersistenceBackend;
import tech.illuin.wombat.persistence.backend.PersistenceInitializer;
import tech.illuin.wombat.persistence.backend.sqlite.backup.SQLiteBackupRestorer;
import tech.illuin.wombat.persistence.backup.BackupProperties;

import java.nio.file.Path;

@ApplicationScoped
@IfBuildProperty(name = "quarkus.datasource.db-kind", stringValue = "sqlite")
public class SQLiteBackendConfig
{
    @Singleton
    public SQLiteProperties provideProperties(
        @ConfigProperty(name = "quarkus.datasource.jdbc.url") String jdbcUrl,
        BackupProperties backupProperties
    ) {
        return new SQLiteProperties(
            jdbcUrl,
            Path.of(jdbcUrl.substring("jdbc:sqlite:".length())),
            backupProperties
        );
    }

    @Singleton
    public PersistenceInitializer provideInitializer(SQLiteProperties properties, Instance<SQLiteBackupRestorer> backupRestorer)
    {
        return new SQLiteInitializer(properties, backupRestorer.isUnsatisfied() ? null : backupRestorer.get());
    }

    @Singleton
    public PersistenceBackend provideBackend(
        Flyway flyway,
        MeterRegistry registry,
        SQLiteSizeGauge sizeGauge
    ) {
        return new SQLiteBackend(flyway, registry, sizeGauge);
    }

    @Singleton
    @IfBuildProperty(name = "backup.enabled", stringValue = "true")
    public SQLiteBackupRestorer provideBackupRestorer(SQLiteProperties sqliteProperties, BackupProperties backupProperties)
    {
        return new SQLiteBackupRestorer(sqliteProperties, backupProperties);
    }

    @Singleton
    public SQLiteSizeGauge provideSizeGauge(AgroalDataSource dataSource)
    {
        return new SQLiteSizeGauge(dataSource);
    }
}
