package tech.illuin.wombat.core.evaluation.impact.commons;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.evaluation.impact.commons.Footprint.FootprintImpact;
import tech.illuin.wombat.core.evaluation.impact.commons.Footprint.FootprintImpact.FootprintImpactItem;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FootprintsTest
{

    @Test
    void sumAddsEmbeddedAndUseComponents()
    {
        FootprintImpact sum = FootprintImpact.sum(List.of(
            impact("kgCO2eq", 1f, 2f),
            impact("kgCO2eq", 3f, 4f)
        ));

        assertEquals("kgCO2eq", sum.unit());
        assertEquals(4f, sum.embedded().value());
        assertEquals(6f, sum.use().value());
        assertEquals(10f, sum.totalValue());
    }

    @Test
    void sumIgnoresNullEntries()
    {
        List<FootprintImpact> impacts = new ArrayList<>();
        impacts.add(null);
        impacts.add(impact("MJ", 5f, 7f));

        FootprintImpact sum = FootprintImpact.sum(impacts);

        assertEquals("MJ", sum.unit());
        assertEquals(5f, sum.embedded().value());
        assertEquals(7f, sum.use().value());
    }

    @Test
    void sumOfEmptyListReturnsNull()
    {
        assertNull(FootprintImpact.sum(List.of()));
    }

    @Test
    void multiplyScalesFootprintImpactItemAndPreservesWarnings()
    {
        List<String> warnings = List.of("Warning 1", "Warning 2");
        FootprintImpactItem item = new FootprintImpactItem(10.0f, warnings);

        FootprintImpactItem scaled = item.multiply(2.5);

        assertEquals(25.0f, scaled.value(), 1e-4);
        assertEquals(warnings, scaled.warnings());
    }

    @Test
    void multiplyScalesFootprintImpactAndPreservesMetadata()
    {
        FootprintImpact impact = new FootprintImpact(
            "kgCO2eq",
            "Carbon emissions",
            new FootprintImpactItem(4.0f, List.of("emb-warn")),
            new FootprintImpactItem(6.0f, List.of("use-warn"))
        );

        FootprintImpact scaled = impact.multiply(1.5);

        assertEquals("kgCO2eq", scaled.unit());
        assertEquals("Carbon emissions", scaled.description());
        assertEquals(6.0f, scaled.embedded().value(), 1e-4);
        assertEquals(List.of("emb-warn"), scaled.embedded().warnings());
        assertEquals(9.0f, scaled.use().value(), 1e-4);
        assertEquals(List.of("use-warn"), scaled.use().warnings());
        assertEquals(15.0f, scaled.totalValue(), 1e-4);
    }

    @Test
    void multiplyScalesFootprintCriteria()
    {
        Footprint footprint = new Footprint(
            impact("kgCO2eq", 1.0f, 2.0f),
            impact("MJ", 10.0f, 20.0f),
            impact("kgSbeq", 0.1f, 0.2f)
        );

        Footprint scaled = footprint.multiply(2.0);

        assertEquals(2.0f, scaled.gwp().embedded().value(), 1e-4);
        assertEquals(4.0f, scaled.gwp().use().value(), 1e-4);
        assertEquals(20.0f, scaled.pe().embedded().value(), 1e-4);
        assertEquals(40.0f, scaled.pe().use().value(), 1e-4);
        assertEquals(0.2f, scaled.adp().embedded().value(), 1e-4);
        assertEquals(0.4f, scaled.adp().use().value(), 1e-4);
    }

    @Test
    void multiplyHandlesNullComponentsAndCriteriaSafely()
    {
        FootprintImpact partialImpact = new FootprintImpact(
            "kgCO2eq",
            "desc",
            new FootprintImpactItem(5.0f, List.of()),
            null
        );

        FootprintImpact scaledImpact = partialImpact.multiply(2.0);
        assertEquals(10.0f, scaledImpact.embedded().value(), 1e-4);
        assertNull(scaledImpact.use());

        Footprint partialFootprint = new Footprint(
            scaledImpact,
            null,
            null
        );

        Footprint scaledFootprint = partialFootprint.multiply(3.0);
        assertEquals(30.0f, scaledFootprint.gwp().embedded().value(), 1e-4);
        assertNull(scaledFootprint.pe());
        assertNull(scaledFootprint.adp());
    }

    @Test
    void multiplyByZeroYieldsZeroValues()
    {
        FootprintImpactItem item = new FootprintImpactItem(15.5f, List.of());
        FootprintImpactItem scaled = item.multiply(0.0);

        assertEquals(0.0f, scaled.value(), 1e-4);
    }

    private static FootprintImpact impact(String unit, float embedded, float use)
    {
        return new FootprintImpact(
            unit,
            "description",
            new FootprintImpactItem(embedded, List.of()),
            new FootprintImpactItem(use, List.of())
        );
    }
}
