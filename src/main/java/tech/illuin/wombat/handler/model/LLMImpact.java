package tech.illuin.wombat.handler.model;

import tech.illuin.wombat.ecologits.model.EcologitsEstimationResponse;

public record LLMImpact(
    EcologitsEstimationResponse estimation,
    int outputTokenCount,
    double requestCount
) {}
