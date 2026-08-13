package tech.illuin.wombat.persistence.backend.sqlite.backup;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Object;
import tech.illuin.wombat.persistence.backend.s3.S3Helper;
import tech.illuin.wombat.persistence.backend.s3.S3Properties;
import tech.illuin.wombat.persistence.backup.BackupCleaner;
import tech.illuin.wombat.persistence.backup.BackupProperties;
import tech.illuin.wombat.persistence.backup.BackupProperties.CleanupProperties;

import java.util.Comparator;
import java.util.List;

/**
 * We need to declare these here as @Scheduled/@Observes will trigger a bean creation regardless of what happen in SQLiteBackendConfig
 */
@Singleton
@IfBuildProperty(name = "quarkus.datasource.db-kind", stringValue = "sqlite")
@IfBuildProperty(name = "backup.cleanup.enabled", stringValue = "true")
public class SQLiteBackupCleaner implements BackupCleaner, AutoCloseable
{
    private final CleanupProperties properties;
    private final S3Properties s3Properties;
    private final S3Client s3Client;

    private static final Logger logger = LoggerFactory.getLogger(SQLiteBackupCleaner.class);

    @Inject
    public SQLiteBackupCleaner(BackupProperties properties)
    {
        this.properties = properties.cleanup();
        this.s3Properties = properties.s3();
        this.s3Client = S3Helper.createClient(this.s3Properties);
    }

    @Scheduled(identity = "sqlite-backup-cleanup", cron = "{backup.cleanup.cron}")
    public void scheduledCleanup()
    {
        this.clean();
    }

    @Override
    public void clean()
    {
        try {
            List<S3Object> objectsToDelete = this.listCleanableObjects();
            logger.debug("Cleanup process found {} backups eligible for removal", objectsToDelete.size());

            int deleted = 0;
            for (S3Object obj : objectsToDelete)
            {
                if (this.deleteObject(obj))
                    deleted++;
            }

            logger.info("Cleanup completed. Deleted {} older backups", deleted);
        }
        catch (SdkException e) {
            logger.error("SQLite backup cleanup failed", e);
        }
    }

    private List<S3Object> listCleanableObjects()
    {
        return this.s3Client
            .listObjectsV2(builder -> builder
                .bucket(this.s3Properties.bucket())
                .prefix(this.s3Properties.keyPrefix())
                .build()
            ).contents().stream()
            .filter(obj -> obj.key().endsWith(".db"))
            .sorted(Comparator.comparing(S3Object::lastModified).reversed())
            .skip(this.properties.retainLast())
            .toList();
    }

    private boolean deleteObject(S3Object object)
    {
        try {
            this.s3Client.deleteObject(builder -> builder
                .bucket(this.s3Properties.bucket())
                .key(object.key())
                .build());

            logger.debug("Deleted old backup: {}", object.key());
            return true;
        }
        catch (SdkException e) {
            logger.error("Failed to delete backup: {}", object.key(), e);
            return false;
        }
    }

    @Override
    @PreDestroy
    public void close()
    {
        this.s3Client.close();
    }
}
