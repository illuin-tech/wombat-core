package tech.illuin.wombat.asset.impl;

import tech.illuin.wombat.asset.AssetHandler;
import tech.illuin.wombat.asset.model.Asset;
import tech.illuin.wombat.asset.model.LLMStaticAsset;
import tech.illuin.wombat.handler.LLMImpactService;
import tech.illuin.wombat.handler.model.LLMImpact;
import tech.illuin.wombat.monitor.AssetProperties;
import tech.illuin.wombat.monitor.AssetType;
import tech.illuin.wombat.persistence.model.TimeRange;
import tech.illuin.wombat.ui.LLMStaticAssetImpact;

import java.util.Collection;

public class LLMStaticAssetHandler implements AssetHandler<LLMStaticAssetImpact>
{
    private final LLMImpactService llmImpactService;

    public LLMStaticAssetHandler(LLMImpactService llmImpactService)
    {
        this.llmImpactService = llmImpactService;
    }

    @Override
    public boolean accept(AssetProperties assetProperties)
    {
        return assetProperties.type() == AssetType.LLM_STATIC;
    }

    @Override
    public LLMStaticAssetImpact computeImpact(Asset asset, Collection<String> assetServices, TimeRange timeRange)
    {
        LLMStaticAsset llmAsset = (LLMStaticAsset) asset;
        LLMImpact impact = this.llmImpactService.computeImpact(llmAsset.properties().id(), llmAsset.profile(), timeRange);
        boolean serviceIncluded = assetServices.isEmpty() || assetServices.contains(llmAsset.profile().model());
        return new LLMStaticAssetImpact(asset.name(), llmAsset.profile(), impact.estimation(), impact.outputTokenCount(), impact.requestCount(), serviceIncluded);
    }
}
