package tech.illuin.wombat.core.source.data;

import tech.illuin.wombat.core.asset.ServiceFamily;

public record LLMData(
    String serviceId,
    String assetId,
    String environmentId,
    String model,
    long outputTokens
) implements MetricData {
    @Override
    public ServiceFamily serviceFamily()
    {
        return ServiceFamily.LLM;
    }
}
