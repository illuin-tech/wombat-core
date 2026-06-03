package tech.illuin.wombat.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import org.flywaydb.core.Flyway;
import tech.illuin.wombat.persistence.backup.BackupProperties;
import tech.illuin.wombat.persistence.backup.SqliteBackupService;

import java.io.File;

@ApplicationScoped
public class PersistenceConfig
{

    static final int STARTUP_PRIORITY_BOOTSTRAP = 1000;

    void onStart(@Observes @Priority(STARTUP_PRIORITY_BOOTSTRAP) StartupEvent event, Flyway flyway)
    {
        new File("data/db").mkdirs();
        flyway.migrate();
    }

    @Singleton
    @IfBuildProperty(name = "persistence.enable-memory-target", stringValue = "true")
    public SQLiteTarget provideSQLiteTarget(DatapointRepository repository, ObjectMapper mapper)
    {
        return new SQLiteTarget(repository, mapper);
    }

    @Singleton
    @IfBuildProperty(name = "backup.enabled", stringValue = "true")
    public SqliteBackupService provideSqliteBackupService(AgroalDataSource dataSource, BackupProperties props)
    {
        return new SqliteBackupService(dataSource, props);
    }
}
