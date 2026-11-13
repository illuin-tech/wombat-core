package tech.illuin.wombat.core.asset;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.type.ServiceFamily;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssetIdentityTest
{
    private static final AssetType TYPE = AssetType.of("tech.illuin", "wombat-module", "llm-static", ActivityRegime.MODELED, ServiceFamily.LLM);
    private static final String TYPE_NAME = TYPE.name();

    @Test
    void of_constructsValidAssetIdentity()
    {
        AssetIdentity identity = AssetIdentity.of("asset-1", "env-prod", "my-asset", TYPE_NAME);
        assertEquals("asset-1", identity.id());
        assertEquals("env-prod", identity.environmentId());
        assertEquals("my-asset", identity.name());
        assertEquals(TYPE_NAME, identity.assetType());
    }

    @Test
    void of_namesTheGivenAssetType()
    {
        assertEquals(AssetIdentity.of("asset-1", "env-prod", "my-asset", TYPE_NAME), AssetIdentity.of("asset-1", "env-prod", "my-asset", TYPE));
    }

    @Test
    void equalityAndHashCode_matchByValues()
    {
        AssetIdentity id1 = AssetIdentity.of("asset-1", "env-prod", "my-asset", TYPE_NAME);
        AssetIdentity id2 = new AssetIdentity("asset-1", "env-prod", "my-asset", TYPE_NAME);
        AssetIdentity id3 = AssetIdentity.of("asset-2", "env-prod", "my-asset", TYPE_NAME);
        AssetIdentity id4 = AssetIdentity.of("asset-1", "env-dev", "my-asset", TYPE_NAME);
        AssetIdentity id5 = AssetIdentity.of("asset-1", "env-prod", "other-asset", TYPE_NAME);
        AssetIdentity id6 = AssetIdentity.of("asset-1", "env-prod", "my-asset", "tech.illuin.wombat-module.other");

        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
        assertNotEquals(id1, id3);
        assertNotEquals(id1, id4);
        assertNotEquals(id1, id5);
        assertNotEquals(id1, id6);
    }

    @Test
    void constructor_rejectsNullOrBlankFields()
    {
        assertThrows(NullPointerException.class, () -> new AssetIdentity(null, "env-prod", "my-asset", TYPE_NAME));
        assertThrows(NullPointerException.class, () -> new AssetIdentity("asset-1", null, "my-asset", TYPE_NAME));
        assertThrows(NullPointerException.class, () -> new AssetIdentity("asset-1", "env-prod", null, TYPE_NAME));
        assertThrows(NullPointerException.class, () -> new AssetIdentity("asset-1", "env-prod", "my-asset", null));

        assertThrows(IllegalArgumentException.class, () -> new AssetIdentity("", "env-prod", "my-asset", TYPE_NAME));
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentity("   ", "env-prod", "my-asset", TYPE_NAME));
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentity("asset-1", "", "my-asset", TYPE_NAME));
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentity("asset-1", "  ", "my-asset", TYPE_NAME));
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentity("asset-1", "env-prod", "", TYPE_NAME));
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentity("asset-1", "env-prod", "   ", TYPE_NAME));
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentity("asset-1", "env-prod", "my-asset", ""));
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentity("asset-1", "env-prod", "my-asset", "  "));
    }
}
