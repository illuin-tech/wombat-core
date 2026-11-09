package tech.illuin.wombat.core.secret;

import java.time.Instant;

public record SecretMetadata(
    String key,
    Instant createdAt,
    Instant updatedAt
) {}
