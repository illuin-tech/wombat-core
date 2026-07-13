package tech.illuin.wombat.monitor;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;
import java.util.Map;

@RegisterForReflection
public record MonitoredEnvironments(Map<String, Environment> environments)
{
    public List<AssetProperties> allAssets()
    {
        return this.environments.values().stream()
            .flatMap(environment -> environment.assets().stream())
            .toList();
    }
}
