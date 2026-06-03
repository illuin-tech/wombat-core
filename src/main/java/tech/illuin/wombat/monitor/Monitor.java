package tech.illuin.wombat.monitor;

import io.quarkus.scheduler.Scheduled;
import tech.illuin.wombat.k8s.K8SResourceHandler;

import java.time.Instant;

public class Monitor
{
    private final K8SResourceHandler k8sResourceHandler;
    private final MonitorProperties properties;

    public Monitor(K8SResourceHandler k8sResourceHandler, MonitorProperties properties)
    {
        this.k8sResourceHandler = k8sResourceHandler;
        this.properties = properties;
    }

    @Scheduled(cron = "${monitor.cron}")
    public void monitor()
    {
        Instant instant = Instant.now();
        this.k8sResourceHandler.handle(instant, this.properties.k8sConfigs().clusters());
    }
}
