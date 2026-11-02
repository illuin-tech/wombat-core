package tech.illuin.wombat.module.llm_static.scenario;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.module.llm_static.scenario.tools.DeterministicJitter;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DeterministicJitterTest
{
    @Test
    void zeroJitterReturnsExactValue()
    {
        ZonedDateTime dt = ZonedDateTime.of(2026, 11, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        assertEquals(5.0, DeterministicJitter.apply(5.0, 0.0, dt));
        assertEquals(0.0, DeterministicJitter.apply(0.0, 0.1, dt));
        assertEquals(5.0, DeterministicJitter.apply(5.0, 0.1, null));
    }

    @Test
    void jitterIsDeterministicForSameTimestamp()
    {
        ZonedDateTime dt = ZonedDateTime.of(2026, 11, 1, 14, 30, 0, 0, ZoneOffset.UTC);
        double val1 = DeterministicJitter.apply(10.0, 0.1, dt);
        double val2 = DeterministicJitter.apply(10.0, 0.1, dt);
        assertEquals(val1, val2);
    }

    @Test
    void jitterStaysWithinConfiguredBounds()
    {
        double base = 100.0;
        double ratio = 0.2; // +/- 20% -> [80.0, 120.0]

        for (int hour = 0; hour < 24; hour++)
        {
            ZonedDateTime dt = ZonedDateTime.of(2026, 11, 1, hour, 0, 0, 0, ZoneOffset.UTC);
            double val = DeterministicJitter.apply(base, ratio, dt);
            assertTrue(val >= 80.0 && val <= 120.0, "Jittered value " + val + " out of expected [80, 120] range");
        }
    }
}
