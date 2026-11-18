package tech.illuin.wombat.core.secret;

import java.util.Objects;
import java.util.Optional;

/**
 * A deferred reference to a secret: it carries the key and the {@link SecretResolver} it came from, never the value.
 * <p>
 * The value is resolved on every {@link #fetch()} or {@link #tryFetch()}, so a rotated credential is picked up without
 * rebuilding whatever holds the handle, and the value lives in memory only for as long as the caller keeps it.
 * Caching, if needed, is the resolver implementation's concern: a handle fetches through whichever resolver produced it.
 */
public final class Secret
{
    private final String key;
    private final SecretResolver resolver;

    Secret(String key, SecretResolver resolver)
    {
        this.key = Objects.requireNonNull(key, "key cannot be null");
        this.resolver = Objects.requireNonNull(resolver, "resolver cannot be null");
    }

    public String key()
    {
        return this.key;
    }

    /**
     * @return the current value of the secret
     * @throws MissingSecretException when the secret is no longer bound
     */
    public String fetch()
    {
        return this.resolver.require(this.key);
    }

    /**
     * @return the current value of the secret, empty when it is no longer bound
     */
    public Optional<String> tryFetch()
    {
        return this.resolver.find(this.key);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (!(o instanceof Secret other))
            return false;
        return this.key.equals(other.key) && this.resolver == other.resolver;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.key, System.identityHashCode(this.resolver));
    }

    @Override
    public String toString()
    {
        return "Secret[" + this.key + "]";
    }
}
