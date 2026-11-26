package tech.illuin.wombat.core.asset.profile.inference;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.profile.AssetProfile;
import tech.illuin.wombat.core.asset.profile.Lifetime;
import tech.illuin.wombat.core.asset.profile.ServerProfile;
import tech.illuin.wombat.core.asset.profile.ServerProvider;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.type.ServiceFamily;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KubernetesProfileResolverTest
{
    private static final AssetType TYPE = AssetType.of("tech.illuin", "test", "kubernetes", ActivityRegime.MEASURED, ServiceFamily.KUBERNETES_CONTAINER);
    private static final Hints NO_HINTS = new Hints(null, null);

    private final ClusterInventory inventory = mock(ClusterInventory.class);
    private final KubernetesProfileInference inference = mock(KubernetesProfileInference.class);
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));
    private final KubernetesProfileResolver resolver = new KubernetesProfileResolver(
        this.inventory,
        asset -> Set.of("payments"),
        this.inference,
        Duration.ofHours(1),
        new Clock() {
            @Override
            public ZoneOffset getZone()
            {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone)
            {
                return this;
            }

            @Override
            public Instant instant()
            {
                return KubernetesProfileResolverTest.this.now.get();
            }
        }
    );

    @Test
    void onlyAcceptsInferableProfiles()
    {
        assertTrue(this.resolver.accept(asset(NO_HINTS)));
        assertFalse(this.resolver.accept(asset(profile("m5.large"))));
    }

    @Test
    void keepsTheInferredProfileUntilItExpires() throws ProfileInferenceException
    {
        when(this.inference.infer(any(), any(), any())).thenReturn(profile("m5.large"), profile("m5.xlarge"));

        AssetProfile first = this.resolver.resolve(asset(NO_HINTS));
        this.now.set(this.now.get().plus(Duration.ofMinutes(59)));
        assertSame(first, this.resolver.resolve(asset(NO_HINTS)));

        this.now.set(this.now.get().plus(Duration.ofMinutes(2)));
        assertNotSame(first, this.resolver.resolve(asset(NO_HINTS)));
        verify(this.inference, times(2)).infer(this.inventory, Set.of("payments"), NO_HINTS);
    }

    @Test
    void changedHintsAreInferredAgain() throws ProfileInferenceException
    {
        when(this.inference.infer(any(), any(), any())).thenReturn(profile("m5.large"), profile("m5.xlarge"));

        AssetProfile first = this.resolver.resolve(asset(NO_HINTS));

        assertNotSame(first, this.resolver.resolve(asset(new Hints(Country.FRANCE, Lifetime.ofYears(5)))));
    }

    @Test
    void failuresAreNotKept() throws ProfileInferenceException
    {
        ServerProfile profile = profile("m5.large");
        when(this.inference.infer(any(), any(), any()))
            .thenThrow(new ProfileInferenceException("nodes unreadable"))
            .thenReturn(profile);

        assertThrows(ProfileInferenceException.class, () -> this.resolver.resolve(asset(NO_HINTS)));
        assertSame(profile, this.resolver.resolve(asset(NO_HINTS)));
    }

    @Test
    void closesItsInventory()
    {
        this.resolver.close();

        verify(this.inventory).close();
    }

    private static ServerProfile profile(String instanceType)
    {
        return new InferredCloudServerProfile(ServerProvider.aws, instanceType, Country.FRANCE, Lifetime.ofYears(4), 1, ProfileOrigin.NODES, List.of());
    }

    private static Asset asset(AssetProfile profile)
    {
        return new Asset() {
            @Override
            public AssetIdentity identity()
            {
                return AssetIdentity.of("cluster", "env", TYPE);
            }

            @Override
            public AssetType type()
            {
                return TYPE;
            }

            @Override
            public AssetProfile profile()
            {
                return profile;
            }
        };
    }

    private record Hints(Country location, Lifetime lifespan) implements InferableServerProfile {}
}
