package tech.illuin.wombat.persistence.backup;

import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record CleanupTestProperties(
    boolean enabled,
    String cron,
    int retainLast
) implements BackupProperties.CleanupProperties {}
