package tech.illuin.wombat.persistence.backend.sqlite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.persistence.backend.s3.S3TestProperties;
import tech.illuin.wombat.persistence.backend.s3.S3TestPropertiesBuilder;
import tech.illuin.wombat.persistence.backend.sqlite.backup.SQLiteBackupRestorer;
import tech.illuin.wombat.persistence.backup.BackupProperties;
import tech.illuin.wombat.persistence.backup.BackupTestPropertiesBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SQLiteInitializerTest
{
    private Path workDir;

    private static final String KEY_PREFIX = "test-restorer/";

    @AfterEach
    void cleanup() throws IOException
    {
        if (this.workDir == null || !Files.exists(this.workDir))
            return;
        try (var paths = Files.walk(this.workDir))
        {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) { /* best effort cleanup */ }
            });
        }
    }

    @Test
    void initialize_createsMissingDatabaseDirectory() throws IOException
    {
        Path dbPath = this.freshDbPath();
        SQLiteProperties props = properties(dbPath, SQLiteTestHelper.createProps(withRestoreOnStartupAndKeyPrefix(false, KEY_PREFIX)));
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
        SQLiteProperties props = properties(dbPath, SQLiteTestHelper.createProps(withRestoreOnStartupAndKeyPrefix(true, KEY_PREFIX)));
        SQLiteInitializer initializer = new SQLiteInitializer(props, backupRestorer);

        initializer.initialize();

        verify(backupRestorer, times(1)).restore();
    }

    @Test
    void initialize_skipsRestore_whenBackupRestorerIsAbsent() throws IOException
    {
        Path dbPath = this.freshDbPath();
        SQLiteProperties props = properties(dbPath, SQLiteTestHelper.createProps(withRestoreOnStartupAndKeyPrefix(true, KEY_PREFIX)));
        SQLiteInitializer initializer = new SQLiteInitializer(props, null);

        initializer.initialize();

        assertTrue(!Files.exists(dbPath), "no backup restorer means no restored database file");
    }

    @Test
    void initialize_skipsRestore_whenRestoreOnStartupIsDisabled() throws IOException
    {
        Path dbPath = this.freshDbPath();
        SQLiteBackupRestorer backupRestorer = mock(SQLiteBackupRestorer.class);
        SQLiteProperties props = properties(dbPath, SQLiteTestHelper.createProps(withRestoreOnStartupAndKeyPrefix(false, KEY_PREFIX)));
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
        SQLiteProperties props = properties(dbPath, SQLiteTestHelper.createProps(withRestoreOnStartupAndKeyPrefix(true, KEY_PREFIX)));
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
        SQLiteProperties props = properties(dbPath, SQLiteTestHelper.createProps(withRestoreOnStartupAndKeyPrefix(true, KEY_PREFIX)));
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

    public static Consumer<BackupTestPropertiesBuilder> withRestoreOnStartupAndKeyPrefix(boolean restoreOnStartup, String keyPrefix)
    {
        return b -> b
            .restoreOnStartup(restoreOnStartup)
            .s3(S3TestPropertiesBuilder.builder((S3TestProperties) b.s3()).keyPrefix(keyPrefix).build());
    }
}
