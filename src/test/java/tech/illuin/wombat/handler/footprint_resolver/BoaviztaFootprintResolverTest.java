package tech.illuin.wombat.handler.footprint_resolver;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactResponse;
import tech.illuin.wombat.model.Footprint;
import tech.illuin.wombat.persistence.model.TimeRange;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoaviztaFootprintResolverTest
{

    private static final String PRORATION_WARNING =
        "This impact has been prorated to the requested time range by the Wombat service";
    private static final String REBALANCE_WARNING =
        "This usage has been rebalanced by the Wombat service, it does not come from the Boavizta API";

    private final BoaviztaFootprintResolver resolver = new BoaviztaFootprintResolver();

    @Test
    void resolveFootprint_fullLifespan_returnsEmbeddedAndUseAsIs()
    {
        BoaviztaInstanceImpactResponse response = impactResponse(100f, 200f);
        TimeRange fullLifespan = new TimeRange(Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-07-01T00:00:00Z"));

        Footprint footprint = resolver.resolveFootprint(response, "svc", 1.0d, fullLifespan, 4344);

        assertEquals(100f, footprint.gwp().embedded().value(), 0.001f);
        assertEquals(200f, footprint.gwp().use().value(), 0.001f);
        assertTrue(footprint.gwp().embedded().warnings().contains(PRORATION_WARNING));
        assertTrue(footprint.gwp().use().warnings().contains(PRORATION_WARNING));
    }

    @Test
    void resolveFootprint_halfLifespan_halvesValues()
    {
        BoaviztaInstanceImpactResponse response = impactResponse(100f, 200f);
        TimeRange halfLifespan = new TimeRange(Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-04-01T00:00:00Z"));

        Footprint footprint = resolver.resolveFootprint(response, "svc", 1.0d, halfLifespan, 4344);

        assertEquals(50f, footprint.gwp().embedded().value(), 0.5f);
        assertEquals(100f, footprint.gwp().use().value(), 1.0f);
    }

    @Test
    void resolveFootprint_partialShare_addsRebalanceWarning()
    {
        BoaviztaInstanceImpactResponse response = impactResponse(100f, 200f);
        TimeRange fullLifespan = new TimeRange(Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-07-01T00:00:00Z"));

        Footprint footprint = resolver.resolveFootprint(response, "svc", 0.25d, fullLifespan, 4344);

        assertEquals(25f, footprint.gwp().embedded().value(), 0.5f);
        assertEquals(50f, footprint.gwp().use().value(), 1.0f);
        assertTrue(footprint.gwp().embedded().warnings().contains(REBALANCE_WARNING));
        assertTrue(footprint.gwp().use().warnings().contains(REBALANCE_WARNING));
    }

    @Test
    void resolveFootprint_fullShare_doesNotAddRebalanceWarning()
    {
        BoaviztaInstanceImpactResponse response = impactResponse(100f, 200f);
        TimeRange fullLifespan = new TimeRange(Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-07-01T00:00:00Z"));

        Footprint footprint = resolver.resolveFootprint(response, "svc", 1.0d, fullLifespan, 4344);

        assertTrue(footprint.gwp().embedded().warnings().stream().noneMatch(REBALANCE_WARNING::equals));
        assertTrue(footprint.gwp().use().warnings().stream().noneMatch(REBALANCE_WARNING::equals));
    }

    @Test
    void resolveFootprint_returnsAllThreeMetrics()
    {
        BoaviztaInstanceImpactResponse response = impactResponse(100f, 200f);
        TimeRange tr = new TimeRange(Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-02-01T00:00:00Z"));

        Footprint footprint = resolver.resolveFootprint(response, "svc", 1.0d, tr, 8760);

        assertNotNull(footprint.gwp());
        assertNotNull(footprint.pe());
        assertNotNull(footprint.adp());
        assertEquals("svc", footprint.service());
    }

    private BoaviztaInstanceImpactResponse impactResponse(float embedded, float use)
    {
        BoaviztaInstanceImpactResponse.Impact impact = new BoaviztaInstanceImpactResponse.Impact(
            "u", "d",
            new BoaviztaInstanceImpactResponse.Impact.ImpactItem(embedded, embedded, embedded, List.of()),
            new BoaviztaInstanceImpactResponse.Impact.ImpactItem(use, use, use, List.of())
        );
        return new BoaviztaInstanceImpactResponse(Map.of("gwp", impact, "pe", impact, "adp", impact), 1);
    }
}
