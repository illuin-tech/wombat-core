package tech.illuin.wombat.k8s;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("profile-id") String profileId,
    @JsonProperty("config-path") String configPath,
    @JsonProperty("namespace") String namespace,
    @JsonProperty("context") Optional<String> context,
    @JsonProperty("read-timeout") Optional<Duration> readTimeout
) implements MonitoredResourceProperties
{
    @Override
    public MonitoredResourceType type()
    {
        return MonitoredResourceType.KUBERNETES;
    }
}
