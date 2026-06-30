package tech.illuin.wombat.monitor;

import io.quarkus.scheduler.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.k8s.ClusterProperties;
import tech.illuin.wombat.k8s.K8SResourceHandler;

import java.time.Instant;
import java.util.List;

public class Monitor
{
    private final K8SResourceHandler k8sResourceHandler;
    private final MonitoredResources resources;

    private static final Logger logger = LoggerFactory.getLogger(Monitor.class);

    public Monitor(K8SResourceHandler k8sResourceHandler, MonitoredResources resources)
    {
        this.k8sResourceHandler = k8sResourceHandler;
        this.resources = resources;
    }

    @Scheduled(cron = "${monitor.cron}")
    public void monitor()
    {
        Instant instant = Instant.now();
        List<ClusterProperties> clusters = this.resources.resources().stream()
            .filter(ClusterProperties.class::isInstance)
            .map(ClusterProperties.class::cast)
            .toList();
        logger.info("Monitor triggered for clusters {}", clusters.stream().map(ClusterProperties::id).toList());
        this.k8sResourceHandler.handle(instant, clusters);
    }
}
