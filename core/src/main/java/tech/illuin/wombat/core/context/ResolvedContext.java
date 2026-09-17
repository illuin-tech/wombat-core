package tech.illuin.wombat.core.context;

import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.Environment;
import tech.illuin.wombat.core.secret.EnvironmentSecretResolver;
import tech.illuin.wombat.core.secret.SecretResolver;

import java.util.List;

public final class ResolvedContext implements WombatContext
{
    private final List<Environment> environments;
    private final SecretResolver secrets;

    public ResolvedContext(List<Environment> environments, SecretResolver secrets)
    {
        this.environments = environments;
        this.secrets = secrets;
    }

    /** Resolves secrets from the process environment, which is where a deployment supplies them. */
    public ResolvedContext(List<Environment> environments)
    {
        this(environments, new EnvironmentSecretResolver());
    }

    public ResolvedContext(Environment... environments)
    {
        this(List.of(environments), new EnvironmentSecretResolver());
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

    @Override
    public SecretResolver secrets()
    {
        return this.secrets;
    }
}
