package tech.illuin.wombat.prometheus;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import tech.illuin.wombat.prometheus.model.PrometheusQueryResponse;

@Path("/api/v1")
public interface PrometheusClient
{

    @GET
    @Path("/query")
    PrometheusQueryResponse query(@QueryParam("query") String query);
}
