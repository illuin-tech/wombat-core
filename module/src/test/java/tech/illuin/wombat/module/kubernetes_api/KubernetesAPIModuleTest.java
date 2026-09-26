package tech.illuin.wombat.module.kubernetes_api;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.context.ResolvedContext;
import tech.illuin.wombat.core.source.WombatSource;
import tech.illuin.wombat.module.kubernetes_api.source.KubernetesAPISource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KubernetesAPIModuleTest
{
    private final KubernetesAPIModule module = new KubernetesAPIModule();

    @Test
    void providesASource()
    {
        // An empty context builds no cluster client, so no kubeconfig is read.
        Optional<WombatSource> source = this.module.createSource(new ResolvedContext());
        assertTrue(source.isPresent());
        assertInstanceOf(KubernetesAPISource.class, source.get());
    }
}
