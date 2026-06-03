package tech.illuin.wombat.monitor;

import java.time.Instant;

public interface MonitoredResourceHandler<M>
{
    void handle(Instant instance, M config);
}
