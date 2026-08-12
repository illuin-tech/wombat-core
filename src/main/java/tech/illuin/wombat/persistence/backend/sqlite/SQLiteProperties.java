package tech.illuin.wombat.persistence.backend.sqlite;

import tech.illuin.wombat.persistence.backup.BackupProperties;

import java.nio.file.Path;

public record SQLiteProperties(
    String jdbcUrl,
    Path dbPath,
    BackupProperties backup
) {}
