package tech.illuin.wombat.module.llm_prometheus;

import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.module.ProcessorFactory;
import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.core.source.WombatSource;
import tech.illuin.wombat.module.llm_prometheus.connector.PrometheusClientConfig;
import tech.illuin.wombat.module.llm_prometheus.source.LLMPrometheusSource;

import java.util.Optional;

public class LLMPrometheusModule implements WombatModule
{
    public static final AssetType TYPE = AssetType.of("tech.illuin", "wombat-module", "llm-prometheus", ActivityRegime.MEASURED, ServiceFamily.LLM);

    @Override
    public AssetType type()
    {
        return TYPE;
    }

    @Override
    public Class<? extends Asset> assetClass()
    {
        return LLMPrometheusAsset.class;
    }

    @Override
    public Optional<ProcessorFactory<WombatSource>> source()
    {
        return Optional.of((services, asset) -> new LLMPrometheusSource(
            PrometheusClientConfig.create((LLMPrometheusAsset) asset, services.secrets())
        ));
    }
}
