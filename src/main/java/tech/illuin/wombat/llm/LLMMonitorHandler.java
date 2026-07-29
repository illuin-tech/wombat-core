package tech.illuin.wombat.llm;

import tech.illuin.wombat.monitor.AssetProperties;
import tech.illuin.wombat.monitor.AssetType;
import tech.illuin.wombat.monitor.MonitoredAssetHandler;

import java.time.Instant;

public class LLMMonitorHandler implements MonitoredAssetHandler<LLMPrometheusProperties>
{
    private final LLMMetricsCollector metricsCollector;

    public LLMMonitorHandler(LLMMetricsCollector metricsCollector)
    {
        this.metricsCollector = metricsCollector;
    }

    @Override
    public boolean accept(AssetProperties config)
    {
        return config.type() == AssetType.LLM_PROMETHEUS;
    }

    @Override
    public void handle(Instant instant, LLMPrometheusProperties config)
    {
        this.metricsCollector.collect(instant, config, config.profile());
    }

    @Override
    public int heartbeatSkip(LLMPrometheusProperties config) {
        return config.heartbeatSkip();
    }
}
