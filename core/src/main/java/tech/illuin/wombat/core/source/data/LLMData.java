package tech.illuin.wombat.core.source.data;

import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.core.asset.type.ServiceFamily;

import java.util.Objects;

public record LLMData(
    String serviceId,
    LLMProvider provider,
    String model,
    Country location,
    long outputTokens,
    long requests
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
