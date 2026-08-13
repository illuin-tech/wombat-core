package tech.illuin.wombat.persistence.backend.sqlite;

import org.testcontainers.containers.localstack.LocalStackContainer;
import tech.illuin.wombat.persistence.backend.s3.S3TestProperties;
import tech.illuin.wombat.persistence.backend.s3.S3TestPropertiesBuilder;
import tech.illuin.wombat.persistence.backup.BackupProperties;
import tech.illuin.wombat.persistence.backup.BackupTestProperties;
import tech.illuin.wombat.persistence.backup.BackupTestPropertiesBuilder;

import java.util.function.Consumer;

public final class SQLiteTestHelper
{
    private SQLiteTestHelper() {}

    public static BackupProperties createProps(Consumer<BackupTestPropertiesBuilder> customizer)
    {
        BackupTestPropertiesBuilder builder = BackupTestProperties.ofDefault();
        customizer.accept(builder);
        return builder.build();
    }

    public static BackupProperties createProps(LocalStackContainer localstack, Consumer<BackupTestPropertiesBuilder> customizer)
    {
        BackupTestPropertiesBuilder builder = BackupTestProperties.ofDefault();
        builder.s3(S3TestPropertiesBuilder.builder((S3TestProperties) builder.s3())
            .endpoint(localstack.getEndpoint().toString())
            .accessKey(localstack.getAccessKey())
            .secretKey(localstack.getSecretKey())
            .build());
        customizer.accept(builder);
        return builder.build();
    }
}
