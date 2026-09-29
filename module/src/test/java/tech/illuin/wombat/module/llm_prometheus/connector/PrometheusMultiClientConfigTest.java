package tech.illuin.wombat.module.llm_prometheus.connector;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.Environment;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.core.context.ResolvedContext;
import tech.illuin.wombat.core.context.WombatContext;
import tech.illuin.wombat.core.secret.EnvironmentSecretResolver;
import tech.illuin.wombat.core.secret.MissingSecretException;
import tech.illuin.wombat.module.llm_prometheus.LLMPrometheusAsset;
import tech.illuin.wombat.module.llm_prometheus.LLMPrometheusProfile;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PrometheusMultiClientConfigTest
{
    @Test
    void buildsAClientPerAssetWithoutAuthentication()
    {
        PrometheusMultiClient multi = PrometheusMultiClientConfig.create(
            context(Map.of(), asset("p", null, null)));

        assertTrue(multi.get("p").isPresent());
    }

    @Test
    void readsTheBasicAuthPasswordFromTheDeclaredVariable()
    {
        PrometheusMultiClient multi = PrometheusMultiClientConfig.create(
            context(Map.of("PROM_PASSWORD", "s3cret"), asset("p", "user", "PROM_PASSWORD")));

        assertTrue(multi.get("p").isPresent());
    }

    @Test
    void failsRatherThanAuthenticatingWithABlankPassword()
    {
        // create() runs at source creation, so an unset variable stops the module wiring rather than producing a
        // client that would 401 on every scrape.
        WombatContext context = context(Map.of(), asset("p", "user", "PROM_PASSWORD"));

        MissingSecretException e = assertThrows(MissingSecretException.class,
            () -> PrometheusMultiClientConfig.create(context));

        assertEquals("PROM_PASSWORD", e.key());
    }

    private static WombatContext context(Map<String, String> environment, LLMPrometheusAsset asset)
    {
        return new ResolvedContext(
            List.of(new Environment("env", List.of(asset))),
            new EnvironmentSecretResolver(environment::get));
    }

    private static LLMPrometheusAsset asset(String id, String username, String passwordEnv)
    {
        return new LLMPrometheusAsset(AssetIdentity.of(id, "env", id), "http://prometheus", null, username, passwordEnv, 0,
            new LLMPrometheusProfile(LLMProvider.mistralai, "m", "FRA", new LLMPrometheusProfile.DynamicProfile("q")));
    }
}
