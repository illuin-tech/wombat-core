package tech.illuin.wombat.core.context;

import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.Environment;

import java.util.List;

public final class ResolvedContext implements WombatContext
{
    private final List<Environment> environments;

    public ResolvedContext(List<Environment> environments)
    {
        this.environments = environments;
    }

    public ResolvedContext(Environment... environments)
    {
        this(List.of(environments));
    }

    @Override
    public List<Environment> environments()
    {
        return this.environments;
    }

    @Override
    public List<Asset> assets()
    {
        return this.environments.stream()
            .flatMap(e -> e.assets().stream())
            .toList();
    }
}
