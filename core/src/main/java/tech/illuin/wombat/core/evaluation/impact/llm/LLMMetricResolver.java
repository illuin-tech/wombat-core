package tech.illuin.wombat.core.evaluation.impact.llm;

import java.util.Collection;
import java.util.Map;

public interface LLMMetricResolver
{
    long sumOutputTokens(long startMs, long endMs, String assetId);

    Map<Long, Double> outputTokensPerBucket(long startMs, long endMs, long stepMs, String assetId, Collection<String> serviceIds);
}
