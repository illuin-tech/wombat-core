package tech.illuin.wombat.ecologits;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import tech.illuin.wombat.ecologits.model.EcologitsEstimationRequest;
import tech.illuin.wombat.ecologits.model.EcologitsEstimationResponse;

@Path("/v1beta")
@RegisterRestClient(configKey = "ecologits-api-url")
public interface EcologitsClient
{

    @POST
    @Path("/estimations")
    EcologitsEstimationResponse estimate(EcologitsEstimationRequest request);
}
