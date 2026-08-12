package tech.illuin.wombat.persistence.backend.s3;

import io.smallrye.config.WithDefault;

public interface S3Properties
{
    String endpoint();

    String bucket();

    @WithDefault("us-east-1")
    String region();

    String keyPrefix();

    String accessKey();

    String secretKey();
}
