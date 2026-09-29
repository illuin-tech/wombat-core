package tech.illuin.wombat.core.activity.llm;

import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.type.ServiceFamily;

import java.util.Set;

public record LLMActivityData(
    ActivityRegime regime,
    Set<String> serviceIds,
    TimeRange range,
    long outputTokenCount,
    int requestCount
) implements ActivityData
{
    @Override public ServiceFamily family()
    {
        return ServiceFamily.LLM;
    }
}
