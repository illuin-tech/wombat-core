package tech.illuin.wombat.persistence.backend.s3;

public interface S3Properties
{
    String endpoint();

    String bucket();

    String keyPrefix();

    String accessKey();

    String secretKey();
}
