package tech.illuin.wombat.core.secret;

import java.security.KeyStore;
import java.util.List;
import java.util.Optional;

/**
 * Resolves a secret by key, so credentials are supplied by the environment instead of being carried in
 * configuration or persisted alongside the assets that use them.
 * <p>
 * Assets reference a secret by its <em>key</em>, never by value: the key is safe to store, log and review, and the
 * value only exists in memory for as long as a client needs it.
 */
public interface SecretResolver
{
    /**
     * @return the secret bound to {@code key}
     * @throws MissingSecretException when nothing is bound, so a misconfigured deployment fails loudly rather than
     *                                authenticating with a blank credential
     */
    String require(String key);

    /**
     * @return the secret bound to {@code key}, empty when nothing is bound to it
     * @throws IllegalArgumentException when {@code key} is null or blank: such a key names no secret at all, which
     *                                  is a caller mistake rather than a missing credential
     */
    Optional<String> find(String key);

    /**
     * Checks that {@code key} is bound now, and returns a handle that resolves it again on each fetch.
     *
     * @return a {@link Secret} backed by this resolver
     * @throws MissingSecretException when nothing is bound to {@code key}
     */
    default Secret requireAsync(String key)
    {
        this.require(key);
        return new Secret(key, this);
    }

    /**
     * Returns a handle that resolves {@code key} on each fetch, this does not check anything upfront.
     *
     * @return a {@link Secret} backed by this resolver
     */
    default Secret async(String key)
    {
        return new Secret(key, this);
    }

    /**
     * Checks whether {@code key} is bound now, and if so returns a handle that resolves it again on each fetch.
     *
     * @return a {@link Secret} backed by this resolver, empty when nothing is bound to {@code key}
     */
    default Optional<Secret> findAsync(String key)
    {
        return this.find(key).map(value -> new Secret(key, this));
    }

    static SecretResolver standard()
    {
        return new EnvironmentSecretResolver();
    }

    static SecretResolver environment()
    {
        return new EnvironmentSecretResolver();
    }

    static SecretResolver composite(SecretResolver... resolvers)
    {
        return new CompositeSecretResolver(resolvers);
    }

    static SecretResolver composite(List<SecretResolver> resolvers)
    {
        return new CompositeSecretResolver(resolvers);
    }

    static SecretResolver keyStore(KeyStore keyStore)
    {
        return new KeyStoreSecretResolver(keyStore);
    }

    static KeyStoreSecretResolver.Builder keyStoreBuilder()
    {
        return KeyStoreSecretResolver.builder();
    }

    static SecretResolver directory(java.nio.file.Path directory)
    {
        return new DirectorySecretResolver(directory);
    }

    static SecretResolver directory(java.io.File directory)
    {
        return new DirectorySecretResolver(directory.toPath());
    }

    static SecretResolver directory(String directoryPath)
    {
        return new DirectorySecretResolver(java.nio.file.Path.of(directoryPath));
    }

    static DirectorySecretResolver.Builder directoryBuilder()
    {
        return DirectorySecretResolver.builder();
    }
}
