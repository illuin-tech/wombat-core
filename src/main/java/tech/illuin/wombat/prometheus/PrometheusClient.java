package tech.illuin.wombat.prometheus;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import tech.illuin.wombat.prometheus.model.PrometheusResponse;

@Path("/query_range")
@RegisterRestClient(configKey = "prometheus-api-url")
public interface PrometheusClient {

    @GET
    PrometheusResponse query(
        @QueryParam("query") String query,
        @QueryParam("step") int step,
        @QueryParam("start") double start,
        @QueryParam("end") double end
    );
}
