package tech.illuin.wombat.module.llm_prometheus;

import tech.illuin.wombat.module.llm_prometheus.connector.PrometheusMultiClientConfig;
import tech.illuin.wombat.module.llm_prometheus.source.LLMPrometheusSource;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetType;
import tech.illuin.wombat.core.context.WombatContext;
import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.core.source.WombatSource;

import java.util.Optional;

public class LLMPrometheusModule implements WombatModule
{
    @Override
    public AssetType type()
    {
        return AssetType.LLM_PROMETHEUS;
    }

    @Override
    public Class<? extends Asset> assetClass()
    {
        return LLMPrometheusAsset.class;
    }

    @Override
    public Optional<WombatSource> createSource(WombatContext context)
    {
        return Optional.of(new LLMPrometheusSource(PrometheusMultiClientConfig.create(context)));
    }
}
