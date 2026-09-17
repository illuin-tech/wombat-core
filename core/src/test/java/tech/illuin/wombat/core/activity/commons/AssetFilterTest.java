package tech.illuin.wombat.core.activity.commons;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetFilterTest
{
    @Test
    void noneAcceptsEveryAssetOfEveryEnvironment()
    {
        AssetFilter filter = AssetFilter.none();

        assertTrue(filter.environments().isEmpty());
        assertTrue(filter.accepts("env", "a1"));
        assertTrue(filter.accepts("other", "a2"));
    }

    @Test
    void anEnvironmentWithoutAssetsAcceptsItWhole()
    {
        AssetFilter filter = AssetFilter.of("env", List.of());

        assertTrue(filter.accepts("env", "a1"));
        assertTrue(filter.accepts("env", "a2"));
        assertFalse(filter.accepts("other", "a1"));
    }

    @Test
    void listedAssetsRestrictTheirEnvironment()
    {
        AssetFilter filter = AssetFilter.of("env", List.of("a1"));

        assertTrue(filter.accepts("env", "a1"));
        assertFalse(filter.accepts("env", "a2"));
        assertFalse(filter.accepts("other", "a1"));
    }

    @Test
    void severalEnvironmentsAreFilteredIndependently()
    {
        AssetFilter filter = new AssetFilter(Set.of(
            new AssetFilter.Environment("env", Set.of(new AssetFilter.Asset("a1", Set.of()))),
            new AssetFilter.Environment("other", Set.of())
        ));

        assertTrue(filter.accepts("env", "a1"));
        assertFalse(filter.accepts("env", "a2"));
        assertTrue(filter.accepts("other", "a2"));
        assertFalse(filter.accepts("third", "a1"));
        assertEquals(Set.of("env", "other"), filter.environmentIds());
    }

    @Test
    void aSingleEnvironmentIsWrappedAndANullOneMeansNoRestriction()
    {
        AssetFilter filter = new AssetFilter(new AssetFilter.Environment("env", Set.of()));
        assertEquals(Set.of("env"), filter.environmentIds());

        AssetFilter empty = new AssetFilter((AssetFilter.Environment) null);
        assertTrue(empty.environments().isEmpty());
        assertTrue(empty.accepts("env", "a1"));
    }

    @Test
    void serviceIdsNarrowAnAssetWithoutExcludingIt()
    {
        AssetFilter filter = new AssetFilter(
            new AssetFilter.Environment("env", Set.of(new AssetFilter.Asset("a1", Set.of("cluster-1"))))
        );

        assertTrue(filter.accepts("env", "a1"));
        assertFalse(filter.accepts("env", "a2"));
        assertEquals(Set.of("cluster-1"), filter.environment("env").orElseThrow().assets().iterator().next().serviceIds());
    }
}
