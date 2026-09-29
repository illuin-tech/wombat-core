package tech.illuin.wombat.core.activity.llm;

import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.type.ServiceFamily;

import java.util.Map;
import java.util.Set;

public record LLMActivityData(
    ActivityRegime regime,
    Set<String> serviceIds,
    TimeRange range,
    Map<String, LLMServiceActivity> services
) implements ActivityData
{
    public LLMActivityData(
        ActivityRegime regime,
        String serviceId,
        TimeRange range,
        long outputTokenCount,
        double requestCount
    ) {
        this(
            regime,
            Set.of(serviceId),
            range,
            Map.of(serviceId, new LLMServiceActivity(null, null, null, outputTokenCount, requestCount))
        );
    }

    public long outputTokenCount()
    {
        return this.services.values().stream().mapToLong(LLMServiceActivity::outputTokenCount).sum();
    }

    public double requestCount()
    {
        return this.services.values().stream().mapToDouble(LLMServiceActivity::requestCount).sum();
    }

    @Override
    public ServiceFamily family()
    {
        return ServiceFamily.LLM;
    }
}
