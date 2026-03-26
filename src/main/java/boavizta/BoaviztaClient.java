package boavizta;

import boavizta.model.BoaviztaInstanceConfigResponse;
import boavizta.model.BoaviztaInstanceImpactRequest;
import boavizta.model.BoaviztaInstanceImpactResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.math.BigInteger;
import java.util.Set;

@Path("/cloud/instance")
@RegisterRestClient(configKey = "boavizta-api-url")
public interface BoaviztaClient {

    @POST
    BoaviztaInstanceImpactResponse getInstanceImpact(
        @QueryParam("verbose") boolean verbose,
        @QueryParam("duration") int duration,
        @QueryParam("criteria") Set<String> criteria,
        BoaviztaInstanceImpactRequest request
    );

    @GET
    @Path("/instance_config")
    BoaviztaInstanceConfigResponse getInstanceConfig(
        @QueryParam("provider") BoaviztaInstanceImpactRequest.Provider provider,
        @QueryParam("instance_type") String instanceType
    );
}
