package tech.illuin.wombat.core.evaluation.impact.llm;

import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.profile.LLMProfile;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.core.connector.ecologits.connector.model.EcologitsEstimationResponse;
import tech.illuin.wombat.core.evaluation.impact.commons.Footprint;
import tech.illuin.wombat.core.evaluation.impact.commons.ServiceImpact;

public record LLMImpact(
    String serviceId,
    AssetType assetType,
    LLMProfile profile,
    LLMProvider provider,
    String model,
    String location,
    double share,
    Footprint footprint,
    EcologitsEstimationResponse estimation,
    long outputTokenCount,
    double requestCount
) implements ServiceImpact {}
