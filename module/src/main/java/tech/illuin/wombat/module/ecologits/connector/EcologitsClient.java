package tech.illuin.wombat.module.ecologits.connector;

import feign.Headers;
import feign.RequestLine;
import tech.illuin.wombat.module.ecologits.connector.model.EcologitsEstimationRequest;
import tech.illuin.wombat.module.ecologits.connector.model.EcologitsEstimationResponse;

public interface EcologitsClient
{
    @RequestLine("POST /v1beta/estimations")
    @Headers("Content-Type: application/json")
    EcologitsEstimationResponse estimate(EcologitsEstimationRequest request);
}
