package tech.illuin.wombat.monitor;

import java.time.Instant;

public interface MonitoredAssetHandler<C extends AssetProperties & Monitored>
{
    boolean accept(AssetProperties config);

    void handle(Instant instant, C config);

    int heartbeatSkip(C config);
}
