package tech.illuin.wombat.monitor;


import io.smallrye.config.ConfigMapping;

import java.util.Map;

@ConfigMapping(prefix = "monitor")
public interface MonitorProperties {
    String cron();

    Map<String, MonitoredKubernetesNamespace> kubernetesConfigs();
}
