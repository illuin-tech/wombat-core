package tech.illuin.wombat.asset.impl;

import tech.illuin.wombat.asset.AssetHandler;
import tech.illuin.wombat.asset.model.Asset;
import tech.illuin.wombat.asset.model.LLMPrometheusAsset;
import tech.illuin.wombat.handler.LLMImpactService;
import tech.illuin.wombat.handler.model.LLMImpact;
import tech.illuin.wombat.monitor.AssetProperties;
import tech.illuin.wombat.monitor.AssetType;
import tech.illuin.wombat.persistence.model.TimeRange;
import tech.illuin.wombat.ui.LLMPrometheusAssetImpact;

import java.util.Collection;

public class LLMPrometheusAssetHandler implements AssetHandler<LLMPrometheusAssetImpact>
{
    private final LLMImpactService llmImpactService;

    public LLMPrometheusAssetHandler(LLMImpactService llmImpactService)
    {
        this.llmImpactService = llmImpactService;
    }

    @Override
    public boolean accept(AssetProperties assetProperties)
    {
        return assetProperties.type() == AssetType.LLM_PROMETHEUS;
    }

    @Override
    public LLMPrometheusAssetImpact computeImpact(Asset asset, Collection<String> assetServices, TimeRange timeRange)
    {
        LLMPrometheusAsset llmAsset = (LLMPrometheusAsset) asset;
        LLMImpact impact = this.llmImpactService.computeImpact(llmAsset.properties().id(), llmAsset.profile(), timeRange);
        boolean serviceIncluded = assetServices.isEmpty() || assetServices.contains(llmAsset.profile().model());
        return new LLMPrometheusAssetImpact(asset.name(), llmAsset.profile(), impact.estimation(), impact.outputTokenCount(), impact.requestCount(), serviceIncluded);
    }
}
