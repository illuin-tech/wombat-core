package tech.illuin.wombat.monitor;

import io.quarkus.scheduler.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.environment.ActiveEnvironments;

import java.time.Instant;

public class Monitor
{
    private final MonitoredAssetHandler handler;
    private final ActiveEnvironments activeEnvironments;

    private static final Logger logger = LoggerFactory.getLogger(Monitor.class);

    public Monitor(MonitoredAssetHandler handler, ActiveEnvironments activeEnvironments)
    {
        this.handler = handler;
        this.activeEnvironments = activeEnvironments;
    }

    @Scheduled(cron = "${monitor.cron}")
    public void monitor()
    {
        Instant instant = Instant.now();
        this.activeEnvironments.allAssets().stream()
            .filter(this.handler::accept)
            .forEach(config -> {
                logger.info("Monitor triggered for clusters {}", config.id());
                this.handler.handle(instant, config);
            });
    }
}
