package tech.illuin.wombat.asset.model.profile;

import tech.illuin.wombat.ecologits.model.EcologitsEstimationRequest;

public sealed interface LLMProfile permits StaticLLMProfile, DynamicLLMProfile
{

    EcologitsEstimationRequest.Provider provider();

    String model();

    String location();
}
