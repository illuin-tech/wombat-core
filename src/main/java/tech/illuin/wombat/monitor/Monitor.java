package tech.illuin.wombat.monitor;

import io.quarkus.scheduler.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Monitor
{
    private final KubernetesResourceHandler kubernetesResourceHandler;
    private final MonitorProperties properties;

    private static final Logger logger = LoggerFactory.getLogger(Monitor.class);

    public Monitor(KubernetesResourceHandler kubernetesResourceHandler, MonitorProperties properties)
    {
        this.kubernetesResourceHandler = kubernetesResourceHandler;
        this.properties = properties;
    }

    @Scheduled(cron = "${monitor.cron}")
    public void monitor()
    {
        this.properties.kubernetesConfigs()
            .forEach((name, config) -> {
                logger.info("Persisting pods usages for kubernetes config {}", name);
                this.kubernetesResourceHandler.handle(name, config);
            });
    }
}
