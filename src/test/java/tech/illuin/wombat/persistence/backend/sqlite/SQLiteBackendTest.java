package tech.illuin.wombat.persistence.backend.sqlite;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SQLiteBackendTest
{
    @Test
    void setup_runsFlywayMigrations()
    {
        Flyway flyway = mock(Flyway.class);
        SQLiteSizeGauge sizeGauge = mock(SQLiteSizeGauge.class);
        SQLiteBackend backend = new SQLiteBackend(flyway, new SimpleMeterRegistry(), sizeGauge);

        backend.setup();

        verify(flyway, times(1)).migrate();
    }

    @Test
    void setup_registersDatabaseSizeGauge()
    {
        Flyway flyway = mock(Flyway.class);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SQLiteSizeGauge sizeGauge = mock(SQLiteSizeGauge.class);
        when(sizeGauge.sizeBytes()).thenReturn(42.0);
        SQLiteBackend backend = new SQLiteBackend(flyway, registry, sizeGauge);

        backend.setup();

        assertNotNull(registry.find("sqlite.db.size.bytes").gauge(), "size gauge should be registered");
        assertEquals(42.0, registry.find("sqlite.db.size.bytes").gauge().value());
    }
}
