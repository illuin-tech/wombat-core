package tech.illuin.wombat.core.source.data;

import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.core.asset.type.ServiceFamily;

import java.util.Objects;

public record LLMData(
    String serviceId,
    LLMProvider provider,
    String model,
    String location,
    long outputTokens
) implements MetricData {
    public LLMData {
        serviceId = Objects.requireNonNull(serviceId);
        provider = Objects.requireNonNull(provider);
        model = Objects.requireNonNull(model);
        location = Objects.requireNonNull(location);
    }

    @Override
    public ServiceFamily serviceFamily()
    {
        return ServiceFamily.LLM;
    }
}
