package tech.illuin.wombat.ui;

import tech.illuin.wombat.monitor.AssetType;
import tech.illuin.wombat.model.Footprint;

public record ServiceImpact(
    String assetName,
    String service,
    String label,
    boolean container,
    boolean llm,
    Footprint footprint
)
{
    public static ServiceImpact of(String assetName, Footprint footprint)
    {
        return new ServiceImpact(
            assetName,
            footprint.service(),
            assetName + " / " + footprint.service(),
            footprint.type() == AssetType.KUBERNETES_API,
            footprint.type() == AssetType.LLM_STATIC || footprint.type() == AssetType.LLM_PROMETHEUS,
            footprint
        );
    }
}
