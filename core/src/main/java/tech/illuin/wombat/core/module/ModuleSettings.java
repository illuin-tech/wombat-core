package tech.illuin.wombat.core.module;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tech.illuin.wombat.core.asset.type.ModuleType;

/**
 * The settings of a module as a whole, as opposed to those of each asset it serves: e.g. the endpoint of the API an
 * impact methodology calls.
 * <p>
 * Described like an asset, a record with Jackson and Jakarta annotations, and named by the type of its module so that a
 * host can read them as documents keyed by {@code type}. The host hands them to the core in {@link WombatServices},
 * from which a factory reads them with {@link WombatServices#settings(ModuleType, Class)}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
public interface ModuleSettings
{
    @JsonIgnore
    ModuleType type();
}
