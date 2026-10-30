package tech.illuin.wombat.core.activity;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.WombatActivityResolver.ActivityResolverType;
import tech.illuin.wombat.core.activity.commons.TimeRange;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WombatActivityResolverTest
{
    private final WombatActivityResolver resolver = (asset, range, filter) -> Optional.empty();

    @Test
    void defaultsToInternalStorage()
    {
        assertEquals(ActivityResolverType.INTERNAL_STORAGE, this.resolver.activityResolverType());
    }

    @Test
    void doesNotProvidePerBucketActivityByDefault()
    {
        TimeRange range = new TimeRange(Instant.EPOCH, Instant.EPOCH.plusSeconds(3600));

        assertThrows(UnsupportedOperationException.class, () -> this.resolver.activityPerBucket(null, range, 3_600_000L, Set.of()));
    }
}

