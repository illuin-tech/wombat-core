package tech.illuin.wombat.persistence.backend.sqlite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import tech.illuin.wombat.persistence.backend.s3.S3Helper;
import tech.illuin.wombat.persistence.backup.BackupProperties;
import tech.illuin.wombat.persistence.backup.BackupRestorer;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.Optional;

public class SQLiteBackupRestorer implements BackupRestorer
{
    private final SQLiteProperties sqliteProperties;
    private final BackupProperties backupProperties;
    private final S3Client s3Client;

    private static final Logger logger = LoggerFactory.getLogger(SQLiteBackupRestorer.class);

    public SQLiteBackupRestorer(SQLiteProperties sqliteProperties, BackupProperties backupProperties)
    {
        this.sqliteProperties = sqliteProperties;
        this.backupProperties = backupProperties;
        this.s3Client = S3Helper.createClient(backupProperties.s3());
    }

    @Override
    public boolean restore()
    {
        Path tmp = null;
        try {
            Optional<S3Object> latest = this.findLatestBackup();
            if (latest.isEmpty())
            {
                logger.info("No SQLite backup found in S3 under prefix {}, skipping restore", this.backupProperties.s3().keyPrefix());
                return false;
            }

            String key = latest.get().key();
            tmp = Files.createTempFile("sqlite-restore-", ".db");
            Files.deleteIfExists(tmp);
            this.download(key, tmp);

            this.overwriteInPlace(tmp, this.sqliteProperties.dbPath());

            logger.info("Restored SQLite database from S3 backup {} to {}", key, this.sqliteProperties.dbPath());
            return true;
        }
        catch (IOException | SdkException e) {
            logger.error("SQLite database restore failed", e);
            return false;
        }
        finally {
            this.deleteSilently(tmp);
        }
    }

    private Optional<S3Object> findLatestBackup()
    {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
            .bucket(this.backupProperties.s3().bucket())
            .prefix(this.backupProperties.s3().keyPrefix())
            .build();

        ListObjectsV2Response response = this.s3Client.listObjectsV2(request);
        return response.contents().stream().max(Comparator.comparing(S3Object::key));
    }

    /**
     * We currently perform an in-place rewrite since the inode may be read by SQLite (although not in use at this point).
     *
     * @param source
     * @param dest
     * @throws IOException
     */
    private void overwriteInPlace(Path source, Path dest) throws IOException
    {
        Path parent = dest.getParent();
        if (parent != null)
            Files.createDirectories(parent);

        try (
            FileChannel in = FileChannel.open(source, StandardOpenOption.READ);
            FileChannel out = FileChannel.open(dest, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
        ) {
            long size = in.size();
            long transferred = 0;
            while (transferred < size)
                transferred += in.transferTo(transferred, size - transferred, out);
        }
    }

    private void download(String key, Path dest) throws IOException
    {
        GetObjectRequest request = GetObjectRequest.builder()
            .bucket(this.backupProperties.s3().bucket())
            .key(key)
            .build();

        this.s3Client.getObject(request, dest);
    }

    private void deleteSilently(Path path)
    {
        try {
            if (path == null)
                return;
            Files.deleteIfExists(path);
            logger.trace("Deleted temp file at {}", path);
        }
        catch (IOException e) {
            logger.warn("Failed to delete temp file at {}", path, e);
        }
    }
}
