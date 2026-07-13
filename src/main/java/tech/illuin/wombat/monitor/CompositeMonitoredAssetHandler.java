package tech.illuin.wombat.monitor;

import java.time.Instant;
import java.util.Collection;
import java.util.NoSuchElementException;

public class CompositeMonitoredAssetHandler implements MonitoredAssetHandler
{
    private final Collection<MonitoredAssetHandler> handlers;

    public CompositeMonitoredAssetHandler(Collection<MonitoredAssetHandler> handlers)
    {
        this.handlers = handlers;
    }

    @Override
    public boolean accept(AssetProperties config) {
        return this.handlers.stream().anyMatch(h -> h.accept(config));
    }

    @Override
    public void handle(Instant instant, AssetProperties config)
    {
        MonitoredAssetHandler handler = this.handlers.stream()
            .filter(h -> h.accept(config))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("No value present"));
        handler.handle(instant, config);
    }
}
