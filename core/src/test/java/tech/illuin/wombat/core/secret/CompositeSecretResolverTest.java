package tech.illuin.wombat.core.secret;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompositeSecretResolverTest
{
    @Test
    void resolvesFromFirstResolverWhenPresent()
    {
        SecretResolver first = new EnvironmentSecretResolver(Map.of("API_KEY", "first-secret")::get);
        SecretResolver second = new EnvironmentSecretResolver(Map.of("API_KEY", "second-secret")::get);

        SecretResolver composite = SecretResolver.composite(first, second);

        assertEquals("first-secret", composite.require("API_KEY"));
        assertEquals("first-secret", composite.find("API_KEY").orElseThrow());
    }

    @Test
    void fallsBackToNextResolverWhenAbsentFromFirst()
    {
        SecretResolver first = new EnvironmentSecretResolver(Map.of("KEY_A", "val-a")::get);
        SecretResolver second = new EnvironmentSecretResolver(Map.of("KEY_B", "val-b")::get);

        SecretResolver composite = SecretResolver.composite(first, second);

        assertEquals("val-a", composite.require("KEY_A"));
        assertEquals("val-b", composite.require("KEY_B"));
    }

    @Test
    void throwsMissingSecretWhenAbsentFromAllResolvers()
    {
        SecretResolver first = new EnvironmentSecretResolver(Map.of("KEY_A", "val-a")::get);
        SecretResolver second = new EnvironmentSecretResolver(Map.of("KEY_B", "val-b")::get);

        SecretResolver composite = new CompositeSecretResolver(List.of(first, second));

        assertTrue(composite.find("UNKNOWN").isEmpty());
        MissingSecretException e = assertThrows(MissingSecretException.class, () -> composite.require("UNKNOWN"));
        assertEquals("UNKNOWN", e.key());
    }

    @Test
    void rejectsNullAndBlankKeys()
    {
        SecretResolver first = new EnvironmentSecretResolver(Map.of("KEY", "val")::get);
        SecretResolver composite = SecretResolver.composite(first);

        assertThrows(IllegalArgumentException.class, () -> composite.find(null));
        assertThrows(IllegalArgumentException.class, () -> composite.find(""));
        assertThrows(IllegalArgumentException.class, () -> composite.find("   "));
        assertThrows(IllegalArgumentException.class, () -> composite.require("   "));
    }

    @Test
    void chainsKeyStoreAndEnvironmentResolvers() throws Exception
    {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);

        SecretKey secretKey = new SecretKeySpec("ks-secret".getBytes(StandardCharsets.UTF_8), "AES");
        ks.setEntry("KEYSTORE_SECRET", new KeyStore.SecretKeyEntry(secretKey), new KeyStore.PasswordProtection("pass".toCharArray()));

        KeyStoreSecretResolver keyStoreResolver = KeyStoreSecretResolver.builder()
            .keyStore(ks)
            .defaultKeyPassword("pass")
            .build();

        EnvironmentSecretResolver envResolver = new EnvironmentSecretResolver(Map.of("ENV_SECRET", "env-val")::get);

        CompositeSecretResolver composite = new CompositeSecretResolver(keyStoreResolver, envResolver);

        assertEquals(2, composite.resolvers().size());
        assertEquals("ks-secret", composite.require("KEYSTORE_SECRET"));
        assertEquals("env-val", composite.require("ENV_SECRET"));
    }

    @Test
    void factoryMethodsProduceWorkingResolvers() throws Exception
    {
        assertNotNull(SecretResolver.standard());
        assertNotNull(SecretResolver.environment());
        assertNotNull(SecretResolver.keyStoreBuilder());

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        assertNotNull(SecretResolver.keyStore(ks));
    }
}
