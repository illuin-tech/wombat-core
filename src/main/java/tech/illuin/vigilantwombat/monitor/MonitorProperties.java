package tech.illuin.vigilantwombat.monitor;


import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "monitor")
public interface MonitorProperties {
    String cron();

    String targetNamespace();
}
