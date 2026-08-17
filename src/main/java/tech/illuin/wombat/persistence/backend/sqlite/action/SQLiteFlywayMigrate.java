package tech.illuin.wombat.persistence.backend.sqlite.action;

import org.flywaydb.core.Flyway;
import tech.illuin.wombat.persistence.backend.api.Action;

public class SQLiteFlywayMigrate implements Action
{
    private final Flyway flyway;

    public SQLiteFlywayMigrate(Flyway flyway)
    {
        this.flyway = flyway;
    }

    @Override
    public void run()
    {
        this.flyway.migrate();
    }
}
