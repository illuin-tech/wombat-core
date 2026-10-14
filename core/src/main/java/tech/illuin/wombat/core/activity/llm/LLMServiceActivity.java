package tech.illuin.wombat.core.activity.llm;

import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.LLMProvider;

public record LLMServiceActivity(
    LLMProvider provider,
    String model,
    Country location,
    long outputTokenCount,
    double requestCount
) {}
