package tech.illuin.wombat.core.secret;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CompositeSecretResolver implements SecretResolver
{
    private final List<SecretResolver> resolvers;

    public CompositeSecretResolver(SecretResolver... resolvers)
    {
        this(List.of(resolvers));
    }

    public CompositeSecretResolver(List<SecretResolver> resolvers)
    {
        Objects.requireNonNull(resolvers, "resolvers cannot be null");
        this.resolvers = List.copyOf(resolvers);
    }

    public List<SecretResolver> resolvers()
    {
        return this.resolvers;
    }

    @Override
    public String require(String key)
    {
        return this.find(key).orElseThrow(() -> new MissingSecretException(key));
    }

    @Override
    public Optional<String> find(String key)
    {
        if (key == null || key.isBlank())
            throw new IllegalArgumentException("key cannot be null or blank");

        for (SecretResolver resolver : this.resolvers)
        {
            Optional<String> secret = resolver.find(key);
            if (secret.isPresent() && !secret.get().isBlank())
                return secret;
        }

        return Optional.empty();
    }
}
