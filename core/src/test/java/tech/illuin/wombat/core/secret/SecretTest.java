package tech.illuin.wombat.core.secret;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.illuin.wombat.core.secret.EnvironmentSecretResolver.PREFIX;

class SecretTest
{
    @Test
    void requireAsyncReturnsAHandleForABoundKey()
    {
        SecretResolver resolver = new EnvironmentSecretResolver(Map.of(PREFIX + "API_KEY", "s3cret")::get);

        Secret secret = resolver.requireAsync("API_KEY");

        assertEquals("API_KEY", secret.key());
        assertEquals("s3cret", secret.fetch());
        assertEquals("s3cret", secret.tryFetch().orElseThrow());
    }

    @Test
    void requireAsyncFailsWhenTheKeyIsUnbound()
    {
        SecretResolver resolver = new EnvironmentSecretResolver(Map.<String, String>of()::get);

        MissingSecretException e = assertThrows(MissingSecretException.class, () -> resolver.requireAsync("API_KEY"));
        assertEquals("API_KEY", e.key());
    }

    @Test
    void findAsyncIsEmptyWhenTheKeyIsUnbound()
    {
        SecretResolver resolver = new EnvironmentSecretResolver(Map.of(PREFIX + "API_KEY", "s3cret")::get);

        assertTrue(resolver.findAsync("API_KEY").isPresent());
        assertTrue(resolver.findAsync("OTHER").isEmpty());
    }

    @Test
    void asyncReturnsAHandleForABoundKey()
    {
        SecretResolver resolver = new EnvironmentSecretResolver(Map.of(PREFIX + "API_KEY", "s3cret")::get);

        Secret secret = resolver.async("API_KEY");

        assertEquals("API_KEY", secret.key());
        assertEquals("s3cret", secret.fetch());
        assertEquals("s3cret", secret.tryFetch().orElseThrow());
    }

    @Test
    void asyncDoesNotCheckTheKeyUpfront()
    {
        SecretResolver resolver = new EnvironmentSecretResolver(Map.<String, String>of()::get);

        Secret secret = resolver.async("API_KEY");

        assertEquals("API_KEY", secret.key());
        assertEquals(Optional.empty(), secret.tryFetch());
        MissingSecretException e = assertThrows(MissingSecretException.class, secret::fetch);
        assertEquals("API_KEY", e.key());
    }

    @Test
    void asyncPicksUpAKeyBoundAfterTheHandleWasCreated()
    {
        Map<String, String> env = new HashMap<>();
        SecretResolver resolver = new EnvironmentSecretResolver(env::get);
        Secret secret = resolver.async("API_KEY");

        env.put(PREFIX + "API_KEY", "late");

        assertEquals("late", secret.fetch());
    }

    @Test
    void asyncHandleEqualsTheOneFromRequireAsync()
    {
        SecretResolver resolver = new EnvironmentSecretResolver(Map.of(PREFIX + "API_KEY", "s3cret")::get);

        assertEquals(resolver.requireAsync("API_KEY"), resolver.async("API_KEY"));
    }

    @Test
    void asyncMethodsRejectNullAndBlankKeys()
    {
        SecretResolver resolver = new EnvironmentSecretResolver(Map.<String, String>of()::get);

        assertThrows(IllegalArgumentException.class, () -> resolver.requireAsync(null));
        assertThrows(IllegalArgumentException.class, () -> resolver.findAsync("  "));
    }

    @Test
    void fetchResolvesTheCurrentValueOnEachCall()
    {
        // The handle carries no value: a rotation or removal after it was obtained shows on the next fetch.
        Map<String, String> env = new HashMap<>(Map.of(PREFIX + "API_KEY", "v1"));
        SecretResolver resolver = new EnvironmentSecretResolver(env::get);
        Secret secret = resolver.requireAsync("API_KEY");

        env.put(PREFIX + "API_KEY", "v2");
        assertEquals("v2", secret.fetch());

        env.remove(PREFIX + "API_KEY");
        assertEquals(Optional.empty(), secret.tryFetch());
        assertThrows(MissingSecretException.class, secret::fetch);
    }

    @Test
    void toStringDoesNotLeakTheValue()
    {
        SecretResolver resolver = new EnvironmentSecretResolver(Map.of(PREFIX + "API_KEY", "s3cret")::get);

        Secret secret = resolver.requireAsync("API_KEY");

        assertFalse(secret.toString().contains("s3cret"));
    }

    @Test
    void handlesAreEqualForTheSameKeyAndResolver()
    {
        SecretResolver resolver = new EnvironmentSecretResolver(Map.of(PREFIX + "A", "a", PREFIX + "B", "b")::get);
        SecretResolver other = new EnvironmentSecretResolver(Map.of(PREFIX + "A", "a")::get);

        assertEquals(resolver.requireAsync("A"), resolver.requireAsync("A"));
        assertEquals(resolver.requireAsync("A").hashCode(), resolver.requireAsync("A").hashCode());
        assertFalse(resolver.requireAsync("A").equals(resolver.requireAsync("B")));
        assertFalse(resolver.requireAsync("A").equals(other.requireAsync("A")));
    }
}
