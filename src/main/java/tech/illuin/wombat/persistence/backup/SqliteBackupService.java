package tech.illuin.wombat.persistence.backup;

import io.agroal.api.AgroalDataSource;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.event.Observes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class SqliteBackupService
{
    private static final Logger logger = LoggerFactory.getLogger(SqliteBackupService.class);
    private static final DateTimeFormatter KEY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");

    private final AgroalDataSource dataSource;
    private final BackupProperties props;
    private final S3Client s3Client;

    public SqliteBackupService(AgroalDataSource dataSource, BackupProperties props)
    {
        this.dataSource = dataSource;
        this.props = props;

        logger.info("SQLite backup enabled — endpoint={} bucket={}", props.s3().endpoint(), props.s3().bucket());

        S3ClientBuilder builder = S3Client.builder()
            .httpClient(UrlConnectionHttpClient.create())
            .endpointOverride(URI.create(props.s3().endpoint()))
            .region(Region.US_EAST_1)
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());

        if (!props.s3().accessKey().isBlank() && !props.s3().secretKey().isBlank())
        {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.s3().accessKey(), props.s3().secretKey())));
        }

        this.s3Client = builder.build();
    }

    @Scheduled(identity = "sqlite-backup", cron = "{backup.cron}")
    void scheduled()
    {
        backup();
    }

    public void onShutdown(@Observes ShutdownEvent event)
    {
        backup();
        s3Client.close();
    }

    void backup()
    {
        Path tmp = null;
        try
        {
            tmp = Files.createTempFile("sqlite-backup-", ".db");
            sqliteBackup(tmp);
            upload(tmp);
        }
        catch (IOException | SQLException | SdkException e) {
            logger.error("SQLite backup failed", e);
        }
        finally {
            deleteSilently(tmp);
        }
    }

    private void deleteSilently(Path path)
    {
        if (path == null) return;
        try
        {
            Files.deleteIfExists(path);
        }
        catch (IOException e) {
            logger.warn("Failed to delete temp file {}", path, e);
        }
    }

    private void sqliteBackup(Path dest) throws SQLException
    {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement())
        {
            stmt.executeUpdate("backup to " + dest.toAbsolutePath());
        }
        logger.debug("SQLite backup written to {}", dest);
    }

    private void upload(Path file) throws IOException
    {
        String key = props.s3().keyPrefix() + "backup_" + LocalDateTime.now(ZoneOffset.UTC).format(KEY_FMT) + ".db";

        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(props.s3().bucket())
            .key(key)
            .contentType("application/octet-stream")
            .build();

        s3Client.putObject(request, file);
        logger.info("Backup uploaded: {} ({} bytes)", key, Files.size(file));
    }
}
