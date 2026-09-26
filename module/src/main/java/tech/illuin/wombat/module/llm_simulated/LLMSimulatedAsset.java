package tech.illuin.wombat.module.llm_simulated;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.activity.llm.LLMActivityData;
import tech.illuin.wombat.core.asset.AssetType;
import tech.illuin.wombat.core.asset.SimulatedAsset;

public record LLMSimulatedAsset(
    @NotNull @Valid LLMActivityData activity,
    @NotNull @JsonProperty("profile") LLMSimulatedProfile profile
) implements SimulatedAsset {
    public AssetType type()
    {
        return LLMSimulatedModule.TYPE;
    }
}
