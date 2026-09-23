package tech.illuin.wombat.module.llm_simulated;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.core.activity.llm.LLMActivityData;
import tech.illuin.wombat.core.asset.AssetType;
import tech.illuin.wombat.core.asset.SimulatedAsset;

public record LLMSimulatedAsset(
    LLMActivityData activity,
    @JsonProperty("profile") LLMSimulatedProfile profile
) implements SimulatedAsset {
    public AssetType type()
    {
        return AssetType.LLM_SIMULATED;
    }
}
