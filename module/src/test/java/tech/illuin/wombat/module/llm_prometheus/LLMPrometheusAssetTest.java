package tech.illuin.wombat.module.llm_prometheus;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.profile.LLMProvider;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LLMPrometheusAssetTest
{
    @Test
    void anAnonymousAssetNeedsNoSecret()
    {
        LLMPrometheusAsset asset = asset(null, null);

        assertFalse(asset.usesBasicAuth());
        assertEquals(Set.of(), asset.requiredSecretKeys());
    }

    @Test
    void aBasicAuthAssetDeclaresTheVariableItNeeds()
    {
        LLMPrometheusAsset asset = asset("user", "PROM_PASSWORD");

        assertTrue(asset.usesBasicAuth());
        assertEquals(Set.of("PROM_PASSWORD"), asset.requiredSecretKeys());
    }

    @Test
    void aUsernameWithoutAPasswordVariableIsRejected()
    {
        // Nothing downstream could resolve a credential for this asset, so the configuration is wrong however the
        // environment is set — surfacing it at startup beats an empty password reaching Prometheus.
        LLMPrometheusAsset asset = asset("user", "  ");

        IllegalStateException e = assertThrows(IllegalStateException.class, asset::requiredSecretKeys);

        assertTrue(e.getMessage().contains("password-env"), e.getMessage());
    }

    private static LLMPrometheusAsset asset(String username, String passwordEnv)
    {
        return new LLMPrometheusAsset("p", "env", "P", "http://prometheus", null, username, passwordEnv, 0,
            new LLMPrometheusProfile(LLMProvider.mistralai, "m", "FRA", new LLMPrometheusProfile.DynamicProfile("q")));
    }
}
