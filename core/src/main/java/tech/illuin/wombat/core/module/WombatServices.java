package tech.illuin.wombat.core.module;

import tech.illuin.wombat.core.module.asset.AssetProcessorFactory;
import tech.illuin.wombat.core.secret.SecretResolver;

import java.util.Objects;

/**
 * System components handed to every {@link AssetProcessorFactory}, whichever asset it builds a processor for.
 * <p>
 * Grouped in a single parameter so that a new service can be introduced without breaking existing factories.
 */
public record WombatServices(
    SecretResolver secrets
) {
    public WombatServices
    {
        Objects.requireNonNull(secrets, "secrets cannot be null");
    }
}
