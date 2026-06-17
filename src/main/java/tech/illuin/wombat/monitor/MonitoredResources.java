package tech.illuin.wombat.monitor;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

/**
 * Root of the monitored-resources file and the injectable holder for the resolved, typed resources.
 */
@RegisterForReflection
public record MonitoredResources(List<MonitoredResourceProperties> resources)
{
}
