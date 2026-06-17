package tech.illuin.wombat.monitor;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * A resource to monitor. Concrete subtypes are selected by the {@code type} discriminator when the
 * monitored-resources file is deserialized by Jackson; the subtypes are registered on the mapper in
 * {@code MonitorConfig} (kept there to avoid a dependency from this package onto the datasource ones).
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface MonitoredResourceProperties
{
    String id();

    String name();

    MonitoredResourceType type();
}
