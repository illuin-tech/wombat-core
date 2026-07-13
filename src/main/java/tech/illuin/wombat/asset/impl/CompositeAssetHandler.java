package tech.illuin.wombat.asset.impl;

import tech.illuin.wombat.asset.model.Asset;
import tech.illuin.wombat.asset.AssetHandler;
import tech.illuin.wombat.monitor.AssetProperties;
import tech.illuin.wombat.persistence.NoCPUUsageException;
import tech.illuin.wombat.persistence.model.TimeRange;
import tech.illuin.wombat.ui.AssetImpact;

import java.util.Collection;

public class CompositeAssetHandler implements AssetHandler<AssetImpact>
{
    private final Collection<AssetHandler<?>> handlers;

    public CompositeAssetHandler(Collection<AssetHandler<?>> handlers)
    {
        this.handlers = handlers;
    }

    @Override
    public boolean accept(AssetProperties assetProperties) {
        return this.handlers.stream().anyMatch(h -> h.accept(assetProperties));
    }

    @Override
    public AssetImpact computeImpact(Asset asset, Collection<String> assetServices, TimeRange timeRange) throws NoCPUUsageException {
        AssetHandler<?> handler = this.handlers.stream()
            .filter(h -> h.accept(asset.properties()))
            .findFirst()
            .orElseThrow();
        return handler.computeImpact(asset, assetServices, timeRange);
    }
}
