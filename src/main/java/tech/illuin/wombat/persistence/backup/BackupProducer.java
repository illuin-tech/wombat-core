package tech.illuin.wombat.persistence.backup;

public interface BackupProducer
{
    /**
     * Backup current datastore state
     */
    void backup();
}
