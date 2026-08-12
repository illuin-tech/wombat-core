package tech.illuin.wombat.persistence.backend.sqlite;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.persistence.backend.PersistenceBackend;

public class SQLiteBackend implements PersistenceBackend
{
    private final SQLiteSizeGauge sizeGauge;
    private final Flyway flyway;
    private final MeterRegistry registry;

    private static final Logger logger = LoggerFactory.getLogger(SQLiteBackend.class);

    public SQLiteBackend(Flyway flyway, MeterRegistry registry, SQLiteSizeGauge sizeGauge)
    {
        this.flyway = flyway;
        this.registry = registry;
        this.sizeGauge = sizeGauge;
    }

    @Override
    public void setup()
    {
        this.setupMigrations();
        this.setupDBMetrics();
    }

    private void setupMigrations()
    {
        logger.info("Setting up SQLite database migrations");
        this.flyway.migrate();
    }

    private void setupDBMetrics()
    {
        logger.info("Setting up SQLite database metrics");

        Gauge.builder("sqlite.db.size.bytes", this.sizeGauge, SQLiteSizeGauge::sizeBytes)
            .description("SQLite database size in bytes")
            .register(this.registry);
    }
}
