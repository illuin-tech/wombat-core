package tech.illuin.wombat.monitor;

import java.time.Instant;

public interface MonitoredAssetHandler
{
    boolean accept(AssetProperties config);

    void handle(Instant instant, AssetProperties config);
}
