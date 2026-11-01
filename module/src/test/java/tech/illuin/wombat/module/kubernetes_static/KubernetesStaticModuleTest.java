package tech.illuin.wombat.module.kubernetes_static;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.module.kubernetes_static.activity.KubernetesStaticActivityResolver;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class KubernetesStaticModuleTest
{
    private final KubernetesStaticModule module = new KubernetesStaticModule();

    @Test
    void declaresTheStaticKubernetesAssetType()
    {
        assertEquals(KubernetesStaticModule.TYPE, this.module.type());
        assertEquals(KubernetesStaticAsset.class, this.module.assetClass());
    }

    @Test
    void providesAStaticActivityResolver()
    {
        Optional<WombatActivityResolver> resolver = this.module.createActivityResolver(null);
        assertTrue(resolver.isPresent());
        assertInstanceOf(KubernetesStaticActivityResolver.class, resolver.get());
    }

    @Test
    void leavesTheImpactToTheFamilyDefault()
    {
        assertTrue(this.module.createImpactResolver(null).isEmpty());
    }
}
