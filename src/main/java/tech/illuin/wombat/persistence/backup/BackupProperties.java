package tech.illuin.wombat.persistence.backup;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "backup")
public interface BackupProperties
{
    @WithDefault("false")
    boolean enabled();

    @WithDefault("0 0 2 * * ?")
    String cron();

    S3 s3();

    interface S3
    {
        String endpoint();

        String bucket();

        String keyPrefix();

        String accessKey();

        String secretKey();
    }
}
