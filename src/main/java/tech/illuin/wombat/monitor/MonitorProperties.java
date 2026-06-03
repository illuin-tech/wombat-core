package tech.illuin.wombat.monitor;


import io.smallrye.config.ConfigMapping;
import tech.illuin.wombat.k8s.K8SProperties;

@ConfigMapping(prefix = "monitor")
public interface MonitorProperties
{
    String cron();

    K8SProperties k8sConfigs();
}
