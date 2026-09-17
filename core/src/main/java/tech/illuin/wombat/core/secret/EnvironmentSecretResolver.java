package tech.illuin.wombat.core.secret;

import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Reads secrets from the process environment.
 * <p>
 * A blank value is treated as absent: an unset variable and one exported empty are the same misconfiguration, and
 * neither should yield a credential.
 */
public class EnvironmentSecretResolver implements SecretResolver
{
    private final UnaryOperator<String> source;

    public EnvironmentSecretResolver()
    {
        this(System::getenv);
    }

    /** Lets tests supply an environment without mutating the JVM's, which cannot be done portably. */
    public EnvironmentSecretResolver(UnaryOperator<String> source)
    {
        this.source = source;
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

        return Optional.ofNullable(this.source.apply(key)).filter(value -> !value.isBlank());
    }
}
