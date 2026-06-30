package tech.illuin.wombat.monitor;


import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "monitor")
public interface MonitorProperties
{
    String cron();

    @WithName("resources-file")
    @WithDefault("monitored/monitored-resources.yaml")
    String resourcesFile();
}
