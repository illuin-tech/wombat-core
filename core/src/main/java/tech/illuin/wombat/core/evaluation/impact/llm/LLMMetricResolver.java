package tech.illuin.wombat.core.evaluation.impact.llm;

import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.activity.llm.LLMServiceActivity;

import java.util.Map;

public interface LLMMetricResolver
{
    Map<Long, Map<String, LLMServiceActivity>> serviceActivitiesPerBucket(TimeRange range, String assetId);
}
