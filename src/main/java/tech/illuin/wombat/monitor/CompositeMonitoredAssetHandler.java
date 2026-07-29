package tech.illuin.wombat.monitor;

import java.time.Instant;
import java.util.Collection;
import java.util.NoSuchElementException;

@SuppressWarnings({"rawtypes", "unchecked"})
public class CompositeMonitoredAssetHandler implements MonitoredAssetHandler
{
    private final Collection<MonitoredAssetHandler> handlers;

    public CompositeMonitoredAssetHandler(Collection<MonitoredAssetHandler> handlers)
    {
        this.handlers = handlers;
    }

    @Override
    public boolean accept(AssetProperties config)
    {
        return this.handlers.stream().anyMatch(handler -> handler.accept(config));
    }

    @Override
    public void handle(Instant instant, AssetProperties config)
    {
        this.handlerFor(config).handle(instant, config);
    }

    @Override
    public int heartbeatSkip(AssetProperties config)
    {
        return this.handlerFor(config).heartbeatSkip(config);
    }

    private MonitoredAssetHandler handlerFor(AssetProperties config)
    {
        return this.handlers.stream()
            .filter(handler -> handler.accept(config))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("No monitored asset handler accepts " + config.type()));
    }
}
