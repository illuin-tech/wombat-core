package tech.illuin.wombat.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import tech.illuin.wombat.persistence.backup.BackupProperties;
import tech.illuin.wombat.persistence.backup.SqliteBackupService;

@ApplicationScoped
public class PersistenceConfig
{
    @Singleton
    @IfBuildProperty(name = "persistence.enable-memory-target", stringValue = "true")
    public MemoryLoadTarget provideMemoryLoadTarget(DatapointRepository repository, ObjectMapper mapper)
    {
        return new MemoryLoadTarget(repository, mapper);
    }

    @Singleton
    @IfBuildProperty(name = "backup.enabled", stringValue = "true")
    public SqliteBackupService provideSqliteBackupService(AgroalDataSource dataSource, BackupProperties props)
    {
        return new SqliteBackupService(dataSource, props);
    }
}
