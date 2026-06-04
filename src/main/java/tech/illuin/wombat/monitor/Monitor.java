package tech.illuin.wombat.monitor;

import io.quarkus.scheduler.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.k8s.K8SProperties;
import tech.illuin.wombat.k8s.K8SResourceHandler;

import java.time.Instant;

public class Monitor
{
    private final K8SResourceHandler k8sResourceHandler;
    private final MonitorProperties properties;

    private static final Logger logger = LoggerFactory.getLogger(Monitor.class);

    public Monitor(K8SResourceHandler k8sResourceHandler, MonitorProperties properties)
    {
        this.k8sResourceHandler = k8sResourceHandler;
        this.properties = properties;
    }

    @Scheduled(cron = "${monitor.cron}")
    public void monitor()
    {
        Instant instant = Instant.now();
        logger.info("Monitor triggered for configs {}", this.properties.k8sConfigs().clusters().stream().map(K8SProperties.ClusterProperties::id).toList());
        this.k8sResourceHandler.handle(instant, this.properties.k8sConfigs().clusters());
    }
}
