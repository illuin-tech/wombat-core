package tech.illuin.wombat.asset.impl;

import tech.illuin.wombat.asset.AssetHandler;
import tech.illuin.wombat.asset.model.Asset;
import tech.illuin.wombat.asset.model.LLMAsset;
import tech.illuin.wombat.handler.LLMImpactService;
import tech.illuin.wombat.handler.model.LLMImpact;
import tech.illuin.wombat.monitor.AssetProperties;
import tech.illuin.wombat.monitor.AssetType;
import tech.illuin.wombat.persistence.model.TimeRange;
import tech.illuin.wombat.ui.LLMAssetImpact;

import java.util.Collection;

public class LLMAssetHandler implements AssetHandler<LLMAssetImpact>
{
    private final LLMImpactService llmImpactService;

    public LLMAssetHandler(LLMImpactService llmImpactService)
    {
        this.llmImpactService = llmImpactService;
    }

    @Override
    public boolean accept(AssetProperties assetProperties)
    {
        return assetProperties.type().equals(AssetType.LLM_STATIC);
    }

    @Override
    public LLMAssetImpact computeImpact(Asset asset, Collection<String> assetServices, TimeRange timeRange)
    {
        LLMAsset llmAsset = (LLMAsset) asset;
        LLMImpact impact = this.llmImpactService.computeImpact(llmAsset.profile(), timeRange);
        boolean serviceIncluded = assetServices.isEmpty() || assetServices.contains(llmAsset.profile().model());
        return new LLMAssetImpact(asset.name(), llmAsset.profile().measureType(), llmAsset.profile(), impact.estimation(), impact.requestCount(), serviceIncluded);
    }
}
