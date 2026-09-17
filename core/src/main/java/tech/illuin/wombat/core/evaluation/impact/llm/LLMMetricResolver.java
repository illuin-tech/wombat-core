package tech.illuin.wombat.core.evaluation.impact.llm;

public interface LLMMetricResolver
{
    long sumOutputTokens(long startMs, long endMs, String assetId);
}
