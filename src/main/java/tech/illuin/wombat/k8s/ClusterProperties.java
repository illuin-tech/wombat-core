package tech.illuin.wombat.k8s;

import io.quarkus.runtime.annotations.RegisterForReflection;
import tech.illuin.wombat.monitor.MonitoredResourceProperties;
import tech.illuin.wombat.monitor.MonitoredResourceType;

import java.time.Duration;
import java.util.Optional;

/**
 * A Kubernetes cluster as a monitored resource ({@link MonitoredResourceType#KUBERNETES}).
 * Holds all Kubernetes-specific connection properties.
 */
@RegisterForReflection
public record ClusterProperties(
    String id,
    String name,
    String profileId,
    String configPath,
    String namespace,
    Optional<String> context,
    Optional<Duration> readTimeout
) implements MonitoredResourceProperties
{
    @Override
    public MonitoredResourceType type()
    {
        return MonitoredResourceType.KUBERNETES;
    }
}
