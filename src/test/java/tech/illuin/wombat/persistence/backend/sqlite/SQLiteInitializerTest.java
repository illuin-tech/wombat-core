package tech.illuin.wombat.persistence.backend.sqlite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.persistence.backend.s3.S3Properties;
import tech.illuin.wombat.persistence.backup.BackupProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SQLiteInitializerTest
{
    private Path workDir;

    @AfterEach
    void cleanup() throws IOException
    {
        if (this.workDir == null || !Files.exists(this.workDir))
            return;
        try (var paths = Files.walk(this.workDir))
        {
            paths.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) { /* best effort cleanup */ }
            });
        }
    }

    @Test
    void initialize_createsMissingDatabaseDirectory() throws IOException
    {
        Path dbPath = this.freshDbPath();
        SQLiteProperties props = properties(dbPath, backupProps(false));
        SQLiteInitializer initializer = new SQLiteInitializer(props, null);

        initializer.initialize();

        assertTrue(Files.exists(dbPath.getParent()), "database directory should have been created");
    }

    @Test
    void initialize_restoresFromLatestBackup_whenRestoreOnStartupEnabledAndDbMissing() throws IOException
    {
        Path dbPath = this.freshDbPath();
        SQLiteBackupRestorer backupRestorer = mock(SQLiteBackupRestorer.class);
        when(backupRestorer.restore()).thenReturn(true);
        SQLiteProperties props = properties(dbPath, backupProps(true));
        SQLiteInitializer initializer = new SQLiteInitializer(props, backupRestorer);

        initializer.initialize();

        verify(backupRestorer, times(1)).restore();
    }

    @Test
    void initialize_skipsRestore_whenBackupRestorerIsAbsent() throws IOException
    {
        Path dbPath = this.freshDbPath();
        SQLiteProperties props = properties(dbPath, backupProps(true));
        SQLiteInitializer initializer = new SQLiteInitializer(props, null);

        initializer.initialize();

        assertTrue(!Files.exists(dbPath), "no backup restorer means no restored database file");
    }

    @Test
    void initialize_skipsRestore_whenRestoreOnStartupIsDisabled() throws IOException
    {
        Path dbPath = this.freshDbPath();
        SQLiteBackupRestorer backupRestorer = mock(SQLiteBackupRestorer.class);
        SQLiteProperties props = properties(dbPath, backupProps(false));
        SQLiteInitializer initializer = new SQLiteInitializer(props, backupRestorer);

        initializer.initialize();

        verify(backupRestorer, never()).restore();
    }

    @Test
    void initialize_skipsRestore_whenDatabaseFileAlreadyExistsWithData() throws IOException
    {
        Path dbPath = this.freshDbPath();
        Files.createDirectories(dbPath.getParent());
        Files.writeString(dbPath, "existing-data");
        SQLiteBackupRestorer backupRestorer = mock(SQLiteBackupRestorer.class);
        SQLiteProperties props = properties(dbPath, backupProps(true));
        SQLiteInitializer initializer = new SQLiteInitializer(props, backupRestorer);

        initializer.initialize();

        verify(backupRestorer, never()).restore();
    }

    @Test
    void initialize_restoresFromLatestBackup_whenDatabaseFileExistsButIsEmpty() throws IOException
    {
        Path dbPath = this.freshDbPath();
        Files.createDirectories(dbPath.getParent());
        Files.createFile(dbPath);
        SQLiteBackupRestorer backupRestorer = mock(SQLiteBackupRestorer.class);
        when(backupRestorer.restore()).thenReturn(true);
        SQLiteProperties props = properties(dbPath, backupProps(true));
        SQLiteInitializer initializer = new SQLiteInitializer(props, backupRestorer);

        initializer.initialize();

        verify(backupRestorer, times(1)).restore();
    }

    private Path freshDbPath() throws IOException
    {
        this.workDir = Files.createTempDirectory("sqlite-initializer-test-");
        Path dbPath = this.workDir.resolve("nested/test.db");
        assertTrue(!Files.exists(dbPath));
        return dbPath;
    }

    private static SQLiteProperties properties(Path dbPath, BackupProperties backup)
    {
        return new SQLiteProperties("jdbc:sqlite:" + dbPath, dbPath, backup);
    }

    private static BackupProperties backupProps(boolean restoreOnStartup)
    {
        return new BackupProperties()
        {
            @Override public boolean enabled() { return true; }

            @Override public String cron() { return "0 0 0 ? * MON#5 2099"; }

            @Override public boolean restoreOnStartup() { return restoreOnStartup; }

            @Override public S3Properties s3()
            {
                return new S3Properties()
                {
                    @Override public String endpoint() { return "http://localhost:0"; }

                    @Override public String bucket() { return "wombat-test"; }

                    @Override public String region() { return "us-east-1"; }

                    @Override public String keyPrefix() { return "tests/"; }

                    @Override public String accessKey() { return ""; }

                    @Override public String secretKey() { return ""; }
                };
            }
        };
    }
}
