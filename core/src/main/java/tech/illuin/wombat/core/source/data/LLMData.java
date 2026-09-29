package tech.illuin.wombat.core.source.data;

import tech.illuin.wombat.core.asset.type.ServiceFamily;

public record LLMData(
    String serviceId,
    String model,
    long outputTokens
) implements MetricData {
    @Override
    public ServiceFamily serviceFamily()
    {
        return ServiceFamily.LLM;
    }
}
