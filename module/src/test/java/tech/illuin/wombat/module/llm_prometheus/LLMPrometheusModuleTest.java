package tech.illuin.wombat.module.llm_prometheus;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.Environment;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.core.context.ResolvedContext;
import tech.illuin.wombat.core.secret.EnvironmentSecretResolver;
import tech.illuin.wombat.core.secret.MissingSecretException;
import tech.illuin.wombat.core.source.WombatSource;
import tech.illuin.wombat.module.llm_prometheus.source.LLMPrometheusSource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LLMPrometheusModuleTest
{
    private final LLMPrometheusModule module = new LLMPrometheusModule();

    @Test
    void declaresThePrometheusLLMAssetType()
    {
        assertEquals(LLMPrometheusModule.TYPE, this.module.type());
        assertEquals(LLMPrometheusAsset.class, this.module.assetClass());
    }

    @Test
    void providesAPrometheusSource()
    {
        // An empty context registers no Prometheus client, so nothing is dialled.
        Optional<WombatSource> source = this.module.createSource(new ResolvedContext());
        assertTrue(source.isPresent());
        assertInstanceOf(LLMPrometheusSource.class, source.get());
    }

    @Test
    void sourcingABasicAuthAssetFailsWhenItsVariableIsUnset()
    {
        // Proves the module builds its clients from the context's resolver: the credential is read from the
        // environment at source creation, never from the asset the reconciler persisted.
        LLMPrometheusAsset asset = new LLMPrometheusAsset(AssetIdentity.of("p", "env", "P"), "http://prometheus", null, "user",
            "PROM_PASSWORD", 0,
            new LLMPrometheusProfile(LLMProvider.mistralai, "m", "FRA", new LLMPrometheusProfile.DynamicProfile("q")));
        ResolvedContext context = new ResolvedContext(
            List.of(new Environment("env", List.of(asset))),
            new EnvironmentSecretResolver(Map.<String, String>of()::get));

        MissingSecretException e = assertThrows(MissingSecretException.class, () -> this.module.createSource(context));

        assertEquals("PROM_PASSWORD", e.key());
    }
}
