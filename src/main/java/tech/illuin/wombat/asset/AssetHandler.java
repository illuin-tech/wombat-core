package tech.illuin.wombat.asset;

import tech.illuin.wombat.asset.model.Asset;
import tech.illuin.wombat.monitor.AssetProperties;
import tech.illuin.wombat.persistence.NoCPUUsageException;
import tech.illuin.wombat.persistence.model.TimeRange;
import tech.illuin.wombat.ui.AssetImpact;

import java.util.Collection;

public interface AssetHandler<I extends AssetImpact>
{
    boolean accept(AssetProperties assetProperties);

    I computeImpact(Asset asset, Collection<String> assetServices, TimeRange timeRange) throws NoCPUUsageException;
}
