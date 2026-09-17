package tech.illuin.wombat.core.evaluation.impact.llm;

import tech.illuin.wombat.core.activity.llm.LLMActivityData;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.profile.LLMProfile;
import tech.illuin.wombat.core.evaluation.impact.commons.AssetImpact;

public interface LLMRequestImpactResolver
{
    AssetImpact resolve(LLMActivityData llmActivityData, Asset asset, LLMProfile profile);
}
