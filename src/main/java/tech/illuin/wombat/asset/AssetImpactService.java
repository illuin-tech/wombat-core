package tech.illuin.wombat.asset;

import tech.illuin.wombat.asset.impl.CompositeAssetHandler;
import tech.illuin.wombat.asset.model.Asset;
import tech.illuin.wombat.persistence.NoCPUUsageException;
import tech.illuin.wombat.persistence.model.TimeRange;
import tech.illuin.wombat.ui.AssetImpact;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AssetImpactService
{
    private final AssetService assetService;
    private final CompositeAssetHandler assetHandler;

    public AssetImpactService(AssetService assetService, CompositeAssetHandler assetHandler)
    {
        this.assetService = assetService;
        this.assetHandler = assetHandler;
    }

    public Map<String, AssetImpact> computeImpacts(String environmentId, List<String> assetIds, TimeRange timeRange) throws NoCPUUsageException
    {
        List<Asset> assets = this.assetService.resolve(environmentId, assetIds);
        if (assets.isEmpty())
            throw new NoAssetsMatchedException("No assets matched the requested environment/ids, and no assets are configured");
        return this.computeImpacts(assets, Map.of(), timeRange);
    }

    public Map<String, AssetImpact> computeImpacts(List<Asset> assets, Map<String, List<String>> servicesByAssetId, TimeRange timeRange) throws NoCPUUsageException
    {
        Map<String, AssetImpact> impacts = new LinkedHashMap<>();
        for (Asset asset : assets)
        {
            String assetId = asset.properties().id();
            impacts.put(assetId, this.assetHandler.computeImpact(asset, servicesByAssetId.getOrDefault(assetId, List.of()), timeRange));
        }
        return impacts;
    }
}
