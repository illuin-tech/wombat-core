package tech.illuin.wombat.monitor;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Placeholder {@link MonitoredResourceProperties} for non-Kubernetes datasources. Carries only the
 * common contract for now; specific fields will be added when a custom resource type is implemented.
 */
@RegisterForReflection
public record CustomResourceProperties(
    String id,
    String name
) implements MonitoredResourceProperties
{
    @Override
    public MonitoredResourceType type()
    {
        return MonitoredResourceType.CUSTOM;
    }
}
