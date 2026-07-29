package tech.illuin.wombat.monitor;

import io.quarkus.scheduler.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.environment.ActiveEnvironments;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({"rawtypes", "unchecked", "checkstyle:IllegalCatch"})
public class Monitor
{
    private final MonitoredAssetHandler handler;
    private final ActiveEnvironments activeEnvironments;
    private final AtomicInteger heartbeatCount;

    private static final Logger logger = LoggerFactory.getLogger(Monitor.class);

    public Monitor(MonitoredAssetHandler handler, ActiveEnvironments activeEnvironments)
    {
        this.handler = handler;
        this.activeEnvironments = activeEnvironments;
        this.heartbeatCount = new AtomicInteger(0);
    }

    @Scheduled(cron = "${monitor.heartbeat}")
    public void monitor()
    {
        Instant instant = Instant.now();
        int beat = this.heartbeatCount.incrementAndGet();
        this.activeEnvironments.allAssets().stream()
            .filter(this.handler::accept)
            .filter(config -> this.acceptHeartbeat(config, beat))
            .forEach(config -> {
                logger.info("Monitor triggered for asset {}", config.id());
                try {
                    this.handler.handle(instant, config);
                }
                catch (Exception e) {
                    logger.error("Monitoring failed for asset {}: {}", config.id(), e.getMessage(), e);
                }
            });
    }

    private boolean acceptHeartbeat(AssetProperties config, int beat)
    {
        int skip = this.handler.heartbeatSkip(config);
        // A skip of 0 or 1 means "run on every heartbeat"; guards against divide-by-zero.
        return skip <= 1 || beat % skip == 0;
    }
}
