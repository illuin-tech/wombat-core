package tech.illuin.wombat.persistence.backup;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@QuarkusTest
class SqliteBackupServiceTest
{

    private static final String BUCKET = "wombat-backups";
    private static final String KEY_PREFIX = "test/";

    private static LocalStackContainer localstack;

    @Inject
    AgroalDataSource dataSource;

    @BeforeAll
    static void startLocalStack()
    {
        assumeTrue(isDockerAvailable(), "Docker is not available — skipping LocalStack-backed backup test");
        localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.4"))
            .withServices(LocalStackContainer.Service.S3);
        localstack.start();

        try (S3Client s3 = adminClient())
        {
            s3.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
        }
    }

    @AfterAll
    static void stopLocalStack()
    {
        if (localstack != null) localstack.stop();
    }

    @Test
    void backup_uploadsSqliteSnapshotToS3()
    {
        BackupProperties props = props();
        SqliteBackupService service = new SqliteBackupService(dataSource, props);

        service.backup();

        try (S3Client s3 = adminClient())
        {
            ListObjectsV2Response listing = s3.listObjectsV2(
                ListObjectsV2Request.builder().bucket(BUCKET).prefix(KEY_PREFIX).build());

            assertEquals(1, listing.contents().size(), "expected one backup file");
            S3Object uploaded = listing.contents().getFirst();
            assertTrue(uploaded.key().startsWith(KEY_PREFIX + "backup_"));
            assertTrue(uploaded.key().endsWith(".db"));
            assertTrue(uploaded.size() > 0L, "uploaded file should not be empty");
        }
    }

    private static boolean isDockerAvailable()
    {
        try
        {
            return DockerClientFactory.instance().isDockerAvailable();
        }
        catch (Throwable t) {
            return false;
        }
    }

    private static S3Client adminClient()
    {
        return S3Client.builder()
            .endpointOverride(URI.create(localstack.getEndpoint().toString()))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
            .region(Region.of(localstack.getRegion()))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build();
    }

    private BackupProperties props()
    {
        return new BackupProperties()
        {
            @Override public boolean enabled() { return true; }

            @Override public String cron() { return "0 0 0 ? * MON#5 2099"; }

            @Override public S3 s3()
            {
                return new S3()
                {
                    @Override public String endpoint() { return localstack.getEndpoint().toString(); }

                    @Override public String bucket() { return BUCKET; }

                    @Override public String keyPrefix() { return KEY_PREFIX; }

                    @Override public String accessKey() { return localstack.getAccessKey(); }

                    @Override public String secretKey() { return localstack.getSecretKey(); }
                };
            }
        };
    }
}
