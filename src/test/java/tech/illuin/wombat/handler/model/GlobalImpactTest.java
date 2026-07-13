package tech.illuin.wombat.handler.model;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.model.Footprint;
import tech.illuin.wombat.model.Footprint.FootprintImpact;
import tech.illuin.wombat.model.Footprint.FootprintImpact.FootprintImpactItem;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GlobalImpactTest
{

    @Test
    void fromSumsEachCriterionAcrossFootprints()
    {
        GlobalImpact global = GlobalImpact.from(List.of(
            footprint(1f, 2f, 10f, 20f, 0.1f, 0.2f),
            footprint(3f, 4f, 30f, 40f, 0.3f, 0.4f)
        ));

        assertEquals(4f, global.gwp().embedded());
        assertEquals(6f, global.gwp().use());
        assertEquals(10f, global.gwp().total());
        assertEquals("kgCO2eq", global.gwp().unit());
        assertEquals(40f, global.pe().embedded());
        assertEquals(60f, global.pe().use());
        assertEquals(100f, global.pe().total());
        assertEquals(0.4f, global.adp().embedded(), 1e-6);
        assertEquals(0.6f, global.adp().use(), 1e-6);
        assertEquals(1f, global.adp().total(), 1e-6);
    }

    @Test
    void fromEmptyListYieldsNullCriteria()
    {
        GlobalImpact global = GlobalImpact.from(List.of());

        assertNull(global.gwp());
        assertNull(global.pe());
        assertNull(global.adp());
    }

    private static Footprint footprint(float gwpEmbedded, float gwpUse, float peEmbedded, float peUse, float adpEmbedded, float adpUse)
    {
        return new Footprint(
            impact("kgCO2eq", gwpEmbedded, gwpUse),
            impact("MJ", peEmbedded, peUse),
            impact("kgSbeq", adpEmbedded, adpUse),
            "service",
            null,
            null
        );
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
