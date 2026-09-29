package tech.illuin.wombat.core.asset;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssetIdentityTest
{
    @Test
    void of_constructsValidAssetIdentity()
    {
        AssetIdentity identity = AssetIdentity.of("asset-1", "env-prod", "my-asset");
        assertEquals("asset-1", identity.id());
        assertEquals("env-prod", identity.environmentId());
        assertEquals("my-asset", identity.name());
    }

    @Test
    void equalityAndHashCode_matchByValues()
    {
        AssetIdentity id1 = AssetIdentity.of("asset-1", "env-prod", "my-asset");
        AssetIdentity id2 = new AssetIdentity("asset-1", "env-prod", "my-asset");
        AssetIdentity id3 = AssetIdentity.of("asset-2", "env-prod", "my-asset");
        AssetIdentity id4 = AssetIdentity.of("asset-1", "env-dev", "my-asset");
        AssetIdentity id5 = AssetIdentity.of("asset-1", "env-prod", "other-asset");

        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
        assertNotEquals(id1, id3);
        assertNotEquals(id1, id4);
        assertNotEquals(id1, id5);
    }

    @Test
    void constructor_rejectsNullOrBlankFields()
    {
        assertThrows(NullPointerException.class, () -> new AssetIdentity(null, "env-prod", "my-asset"));
        assertThrows(NullPointerException.class, () -> new AssetIdentity("asset-1", null, "my-asset"));
        assertThrows(NullPointerException.class, () -> new AssetIdentity("asset-1", "env-prod", null));

        assertThrows(IllegalArgumentException.class, () -> new AssetIdentity("", "env-prod", "my-asset"));
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentity("   ", "env-prod", "my-asset"));
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentity("asset-1", "", "my-asset"));
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentity("asset-1", "  ", "my-asset"));
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentity("asset-1", "env-prod", ""));
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentity("asset-1", "env-prod", "   "));
    }
}
