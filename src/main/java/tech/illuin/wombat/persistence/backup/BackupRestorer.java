package tech.illuin.wombat.persistence.backup;

public interface BackupRestorer
{
    /**
     * Restore datastore to latest saved backup
     */
    boolean restore();
}
