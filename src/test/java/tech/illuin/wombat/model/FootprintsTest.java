package tech.illuin.wombat.model;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.model.Footprint.FootprintImpact;
import tech.illuin.wombat.model.Footprint.FootprintImpact.FootprintImpactItem;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FootprintsTest
{

    @Test
    void sumAddsEmbeddedAndUseComponents()
    {
        FootprintImpact sum = Footprints.sum(List.of(
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

        FootprintImpact sum = Footprints.sum(impacts);

        assertEquals("MJ", sum.unit());
        assertEquals(5f, sum.embedded().value());
        assertEquals(7f, sum.use().value());
    }

    @Test
    void sumOfEmptyListReturnsNull()
    {
        assertNull(Footprints.sum(List.of()));
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
