package tech.illuin.wombat.persistence.backup;

public interface BackupCleaner
{
    /**
     * Cleanup old backups
     */
    void clean();
}
