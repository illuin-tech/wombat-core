package tech.illuin.wombat.core.connector.boavizta.connector.model;

import tech.illuin.wombat.core.asset.profile.ServerProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum BoaviztaServerProvider
{
    aws, azure, gcp, ovhcloud, scaleway;

    private static final Map<String, BoaviztaServerProvider> index;

    static
    {
        index = new HashMap<>();
        for (BoaviztaServerProvider p : BoaviztaServerProvider.values())
        {
            index.put(p.name(), p);
        }
    }

    public static Optional<BoaviztaServerProvider> forName(ServerProvider provider)
    {
        return Optional.ofNullable(index.get(provider.name()));
    }
}
