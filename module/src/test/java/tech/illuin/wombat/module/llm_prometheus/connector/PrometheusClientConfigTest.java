package tech.illuin.wombat.module.llm_prometheus.connector;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.module.llm_prometheus.LLMPrometheusModule;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.core.secret.EnvironmentSecretResolver;
import tech.illuin.wombat.core.secret.MissingSecretException;
import tech.illuin.wombat.core.secret.SecretResolver;
import tech.illuin.wombat.core.source.Period;
import tech.illuin.wombat.core.secret.SecretKey;
import tech.illuin.wombat.module.llm_prometheus.LLMPrometheusAsset;
import tech.illuin.wombat.module.llm_prometheus.LLMPrometheusProfile;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tech.illuin.wombat.core.activity.commons.Country.FRANCE;

public class PrometheusClientConfigTest
{
    @Test
    void buildsAClientWithoutAuthentication()
    {
        PrometheusClient client = PrometheusClientConfig.create(asset("p", null, null), secrets(Map.of()));
        assertNotNull(client);
    }

    @Test
    void readsTheBasicAuthPasswordFromTheDeclaredVariable()
    {
        PrometheusClient client = PrometheusClientConfig.create(asset("p", "user", "PROM_PASSWORD"), secrets(Map.of(EnvironmentSecretResolver.PREFIX + "PROM_PASSWORD", "s3cret")));
        assertNotNull(client);
    }

    @Test
    void failsRatherThanAuthenticatingWithABlankPassword()
    {
        // create() runs at source creation, so an unset variable fails the source rather than producing a client
        // that would 401 on every scrape.
        SecretResolver secrets = secrets(Map.of());
        MissingSecretException e = assertThrows(MissingSecretException.class, () -> PrometheusClientConfig.create(asset("p", "user", "PROM_PASSWORD"), secrets));

        assertEquals("PROM_PASSWORD", e.key());
    }

    private static SecretResolver secrets(Map<String, String> environment)
    {
        return new EnvironmentSecretResolver(environment::get);
    }

    private static LLMPrometheusAsset asset(String id, String username, String passwordEnv)
    {
        return new LLMPrometheusAsset(
            AssetIdentity.of(id, "env", LLMPrometheusModule.TYPE),
            "http://prometheus", null, username, SecretKey.of(passwordEnv), Period.ZERO,
            new LLMPrometheusProfile(LLMProvider.mistralai, "m", FRANCE, new LLMPrometheusProfile.DynamicProfile("q"))
        );
    }
}
