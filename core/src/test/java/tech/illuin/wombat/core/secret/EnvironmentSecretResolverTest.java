package tech.illuin.wombat.core.secret;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.illuin.wombat.core.secret.EnvironmentSecretResolver.PREFIX;

class EnvironmentSecretResolverTest
{
    @Test
    void resolvesAnExportedValue()
    {
        SecretResolver resolver = resolver(Map.of(PREFIX + "PROM_PASSWORD", "s3cret"));

        assertEquals("s3cret", resolver.require("PROM_PASSWORD"));
        assertEquals("s3cret", resolver.find("PROM_PASSWORD").orElseThrow());
    }

    @Test
    void ignoresAnUnprefixedVariable()
    {
        SecretResolver resolver = resolver(Map.of("PROM_PASSWORD", "s3cret"));

        assertTrue(resolver.find("PROM_PASSWORD").isEmpty());
        assertThrows(MissingSecretException.class, () -> resolver.require("PROM_PASSWORD"));
    }

    @Test
    void keepsTheProcessCredentialsOutOfReach()
    {
        // Whoever writes an asset picks its keys: none may reach a variable the app reads for itself, including one
        // under a lookalike prefix such as the database KEK's.
        SecretResolver resolver = resolver(Map.of(
            "QUARKUS_OIDC_CREDENTIALS_SECRET", "oidc",
            "WOMBAT_SECRET_DATABASE_KEK_CURRENT", "kek"
        ));

        assertTrue(resolver.find("QUARKUS_OIDC_CREDENTIALS_SECRET").isEmpty());
        assertTrue(resolver.find("WOMBAT_SECRET_DATABASE_KEK_CURRENT").isEmpty());
        assertTrue(resolver.find("DATABASE_KEK_CURRENT").isEmpty());
    }

    @Test
    void treatsABlankValueAsAbsent()
    {
        // A variable exported empty is the same misconfiguration as one never exported: an interceptor built from it
        // would authenticate with an empty password and fail at the far end, far from the cause.
        SecretResolver resolver = resolver(Map.of(PREFIX + "EMPTY", "", PREFIX + "SPACES", "   "));

        assertTrue(resolver.find("EMPTY").isEmpty());
        assertTrue(resolver.find("SPACES").isEmpty());
        assertThrows(MissingSecretException.class, () -> resolver.require("EMPTY"));
    }

    @Test
    void requireNamesTheMissingKey()
    {
        SecretResolver resolver = resolver(Map.of());

        MissingSecretException e = assertThrows(MissingSecretException.class, () -> resolver.require("PROM_PASSWORD"));

        assertEquals("PROM_PASSWORD", e.key());
        assertTrue(e.getMessage().contains("PROM_PASSWORD"), e.getMessage());
    }

    @Test
    void rejectsAnUnnamedKey()
    {
        // An asset that declares basic auth without a password-key would otherwise look up the empty variable name.
        SecretResolver resolver = resolver(Map.of(PREFIX + "PROM_PASSWORD", "s3cret"));

        assertThrows(IllegalArgumentException.class, () -> resolver.find(null));
        assertThrows(IllegalArgumentException.class, () -> resolver.find(""));
        assertThrows(IllegalArgumentException.class, () -> resolver.find("  "));
        assertThrows(IllegalArgumentException.class, () -> resolver.require(""));
    }

    private static SecretResolver resolver(Map<String, String> environment)
    {
        return new EnvironmentSecretResolver(environment::get);
    }
}
