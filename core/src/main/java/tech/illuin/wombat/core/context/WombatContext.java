package tech.illuin.wombat.core.context;

import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.Environment;
import tech.illuin.wombat.core.secret.SecretResolver;

import java.util.List;

public interface WombatContext
{
    List<Environment> environments();

    List<Asset> assets();

    /**
     * Resolves the credentials the context's assets reference by variable name.
     * <p>
     * Part of the context rather than a per-module dependency: an asset and the secrets it needs are resolved
     * together, so a module builds its clients from the context alone.
     */
    SecretResolver secrets();

    default WombatContext scope(AssetType type)
    {
        return new ResolvedContext(
            this.environments().stream()
                .map(e -> {
                    List<Asset> filtered = e.assets().stream()
                        .filter(p -> p.type().equals(type))
                        .toList();
                    return new Environment(e.id(), filtered);
                })
                .filter(e -> !e.assets().isEmpty())
                .toList(),
            this.secrets()
        );
    }
}
