package tech.illuin.wombat.core.secret;

import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Reads secrets from the process environment, under the {@link #PREFIX} only: the key {@code PROM_PASSWORD} reads the
 * variable {@code WOMBAT_ASSET_SECRET_PROM_PASSWORD}.
 * <p>
 * Keys come from asset configuration, which users may write: the prefix keeps the variables the process holds for
 * itself (database, OIDC or encryption credentials) out of their reach. Keys do not carry it, so the same key resolves
 * from any other {@link SecretResolver}.
 * <p>
 * A blank value is treated as absent: an unset variable and one exported empty are the same misconfiguration, and
 * neither should yield a credential.
 */
public class EnvironmentSecretResolver implements SecretResolver
{
    private final UnaryOperator<String> source;

    public static final String PREFIX = "WOMBAT_ASSET_SECRET_";

    public EnvironmentSecretResolver()
    {
        this(System::getenv);
    }

    /**
     * Lets tests supply an environment without mutating the JVM's, which cannot be done portably. The source is read
     * by variable name, prefix included.
     */
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

        return Optional.ofNullable(this.source.apply(PREFIX + key)).filter(value -> !value.isBlank());
    }
}
