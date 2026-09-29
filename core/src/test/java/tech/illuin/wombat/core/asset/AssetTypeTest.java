package tech.illuin.wombat.core.asset;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.type.ServiceFamily;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssetTypeTest
{
    @Test
    void of_constructsValidAssetType()
    {
        AssetType type = AssetType.of("tech.illuin", "llm-prometheus", ActivityRegime.MEASURED, ServiceFamily.LLM);
        assertEquals("tech.illuin", type.namespace());
        assertEquals("llm-prometheus", type.localName());
        assertEquals(ActivityRegime.MEASURED, type.regime());
        assertEquals(ServiceFamily.LLM, type.family());
        assertEquals("tech.illuin.llm-prometheus", type.name());
    }

    @Test
    void of_withGroupAndArtifact_constructsValidNamespace()
    {
        AssetType type = AssetType.of("tech.illuin", "wombat-module", "kubernetes-api", ActivityRegime.MEASURED, ServiceFamily.KUBERNETES_CONTAINER);
        assertEquals("tech.illuin.wombat-module", type.namespace());
        assertEquals("kubernetes-api", type.localName());
        assertEquals("tech.illuin.wombat-module.kubernetes-api", type.name());
    }

    @Test
    void equalityAndHashCode_matchByValues()
    {
        AssetType type1 = AssetType.of("tech.illuin.wombat-module", "llm-prometheus", ActivityRegime.MEASURED, ServiceFamily.LLM);
        AssetType type2 = AssetType.of("tech.illuin", "wombat-module", "llm-prometheus", ActivityRegime.MEASURED, ServiceFamily.LLM);
        AssetType type3 = AssetType.of("tech.illuin.wombat-module", "llm-static", ActivityRegime.MODELED, ServiceFamily.LLM);

        assertEquals(type1, type2);
        assertEquals(type1.hashCode(), type2.hashCode());
        assertNotEquals(type1, type3);
    }

    @Test
    void constructor_rejectsNullOrBlankFields()
    {
        assertThrows(NullPointerException.class, () -> new AssetType(null, "llm", ActivityRegime.MEASURED, ServiceFamily.LLM));
        assertThrows(NullPointerException.class, () -> new AssetType("ns", null, ActivityRegime.MEASURED, ServiceFamily.LLM));
        assertThrows(NullPointerException.class, () -> new AssetType("ns", "llm", null, ServiceFamily.LLM));
        assertThrows(NullPointerException.class, () -> new AssetType("ns", "llm", ActivityRegime.MEASURED, null));

        assertThrows(IllegalArgumentException.class, () -> new AssetType("", "llm", ActivityRegime.MEASURED, ServiceFamily.LLM));
        assertThrows(IllegalArgumentException.class, () -> new AssetType("   ", "llm", ActivityRegime.MEASURED, ServiceFamily.LLM));
        assertThrows(IllegalArgumentException.class, () -> new AssetType("ns", "", ActivityRegime.MEASURED, ServiceFamily.LLM));
        assertThrows(IllegalArgumentException.class, () -> new AssetType("ns", "  ", ActivityRegime.MEASURED, ServiceFamily.LLM));
    }
}
