package tech.illuin.wombat.module.llm_prometheus.connector;

import feign.Param;
import feign.RequestLine;
import tech.illuin.wombat.module.llm_prometheus.connector.model.PrometheusQueryResponse;

public interface PrometheusClient
{
    @RequestLine("GET /api/v1/query?query={query}")
    PrometheusQueryResponse query(@Param("query") String query);
}
