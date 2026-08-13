package tech.illuin.wombat.persistence.backend.sqlite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.persistence.backend.PersistenceInitializer;
import tech.illuin.wombat.persistence.backend.sqlite.backup.SQLiteBackupRestorer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SQLiteInitializer implements PersistenceInitializer
{
    private final SQLiteProperties properties;
    private final SQLiteBackupRestorer backupRestorer;

    private static final Logger logger = LoggerFactory.getLogger(SQLiteInitializer.class);

    public SQLiteInitializer(SQLiteProperties properties, SQLiteBackupRestorer backupRestorer)
    {
        this.properties = properties;
        this.backupRestorer = backupRestorer;
    }

    @Override
    public void initialize()
    {
        this.initDirectory();
        this.initBackup();
    }

    private void initDirectory()
    {
        logger.info("Ensuring database directory exists");
        this.properties.dbPath().getParent().toFile().mkdirs();
    }

    private void initBackup()
    {
        logger.info("Database backup is {}", this.properties.backup().enabled() ? "enabled" : "disabled");

        /* If backup restoration is enabled */
        if (!this.properties.backup().restoreOnStartup() || this.backupRestorer == null)
            logger.info("Database restoration is disabled");
        else if (this.isDatabaseMissingOrEmpty())
            logger.info("Database file already exists, skipping restoration");
        else {
            logger.info("Attempting database restoration from remote backup");
            this.backupRestorer.restore();
        }
    }

    private boolean isDatabaseMissingOrEmpty()
    {
        Path dbPath = this.properties.dbPath();
        if (!Files.exists(dbPath))
            return false;
        try {
            return Files.size(dbPath) > 0;
        }
        catch (IOException e) {
            logger.warn("Failed to check database file size at {}, assuming it already has data", dbPath, e);
            return true;
        }
    }
}
