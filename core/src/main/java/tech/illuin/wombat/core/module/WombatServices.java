package tech.illuin.wombat.core.module;

import tech.illuin.wombat.core.asset.type.ModuleType;
import tech.illuin.wombat.core.secret.SecretResolver;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * System components handed to every {@link ProcessorFactory}, whichever asset it builds a processor for.
 * <p>
 * Grouped in a single parameter so that a new service can be introduced without breaking existing factories.
 *
 * @param secrets  the credentials assets reference by key
 * @param metrics  the metrics sourced so far, for the components resolving activity out of them
 * @param settings the settings of the modules that have some, at most one per module; a module given none applies its
 *                 defaults
 */
public record WombatServices(
    SecretResolver secrets,
    MetricResolvers metrics,
    List<ModuleSettings> settings
) {
    public WombatServices
    {
        Objects.requireNonNull(secrets, "secrets cannot be null");
        Objects.requireNonNull(metrics, "metrics cannot be null");
        settings = List.copyOf(Objects.requireNonNull(settings, "settings cannot be null"));

        Set<String> names = new HashSet<>();
        for (ModuleSettings entry : settings)
        {
            if (!names.add(entry.type().name()))
                throw new IllegalArgumentException("settings were given twice for module " + entry.type().name());
        }
    }

    /**
     * @return the settings given for the module of type {@code type}, empty when it was given none
     */
    public Optional<ModuleSettings> settings(ModuleType type)
    {
        return this.settings.stream()
            .filter(entry -> entry.type().name().equals(type.name()))
            .findFirst();
    }

    /**
     * @return the settings given for the module of type {@code type}, cast to {@code expected}, empty when it was given none
     */
    public <S extends ModuleSettings> Optional<S> settings(ModuleType type, Class<S> expected)
    {
        return this.settings(type).map(expected::cast);
    }
}
