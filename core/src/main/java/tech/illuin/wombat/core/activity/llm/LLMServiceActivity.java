package tech.illuin.wombat.core.activity.llm;

import tech.illuin.wombat.core.asset.profile.LLMProvider;

public record LLMServiceActivity(
    LLMProvider provider,
    String model,
    String location,
    long outputTokenCount,
    double requestCount
) {}
