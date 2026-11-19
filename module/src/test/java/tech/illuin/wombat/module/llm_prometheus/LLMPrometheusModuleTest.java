package tech.illuin.wombat.module.llm_prometheus;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.core.module.ProcessorFactory;
import tech.illuin.wombat.core.module.WombatServices;
import tech.illuin.wombat.core.secret.EnvironmentSecretResolver;
import tech.illuin.wombat.core.secret.MissingSecretException;
import tech.illuin.wombat.core.source.Period;
import tech.illuin.wombat.core.source.WombatSource;
import tech.illuin.wombat.core.secret.SecretKey;
import tech.illuin.wombat.module.llm_prometheus.source.LLMPrometheusSource;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.illuin.wombat.core.activity.commons.Country.FRANCE;

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
        // Building the client does not dial Prometheus.
        Optional<ProcessorFactory<WombatSource>> factory = this.module.source();
        assertTrue(factory.isPresent());
        assertInstanceOf(LLMPrometheusSource.class, factory.get().create(services(Map.of()), asset(null, null)));
    }

    @Test
    void sourcingABasicAuthAssetFailsWhenItsVariableIsUnset()
    {
        // Proves the module builds its clients from the services' resolver: the credential is read from the
        // environment at source creation, never from the asset the reconciler persisted.
        ProcessorFactory<WombatSource> factory = this.module.source().orElseThrow();
        MissingSecretException e = assertThrows(MissingSecretException.class, () -> factory.create(services(Map.of()), asset("user", "PROM_PASSWORD")));

        assertEquals("PROM_PASSWORD", e.key());
    }

    private static WombatServices services(Map<String, String> environment)
    {
        return new WombatServices(new EnvironmentSecretResolver(environment::get));
    }

    private static LLMPrometheusAsset asset(String username, String passwordEnv)
    {
        return new LLMPrometheusAsset(
            AssetIdentity.of("p", "env", LLMPrometheusModule.TYPE),
            "http://prometheus", null, username, SecretKey.of(passwordEnv), Period.ZERO,
            new LLMPrometheusProfile(LLMProvider.mistralai, "m", FRANCE, new LLMPrometheusProfile.DynamicProfile("q"))
        );
    }
}
