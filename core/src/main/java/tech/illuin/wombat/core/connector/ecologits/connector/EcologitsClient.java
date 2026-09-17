package tech.illuin.wombat.core.connector.ecologits.connector;

import feign.Headers;
import feign.RequestLine;
import tech.illuin.wombat.core.connector.ecologits.connector.model.EcologitsEstimationRequest;
import tech.illuin.wombat.core.connector.ecologits.connector.model.EcologitsEstimationResponse;

public interface EcologitsClient
{
    @RequestLine("POST /v1beta/estimations")
    @Headers("Content-Type: application/json")
    EcologitsEstimationResponse estimate(EcologitsEstimationRequest request);
}
