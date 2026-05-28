package tech.illuin.wombat.prometheus;

import tech.illuin.wombat.prometheus.model.PrometheusResponse;

import java.time.Instant;

public class PrometheusService {

    private static final String PROMETHEUS_QUERY = "sum(container_cpu_value)";

    private final PrometheusClient client;

    public PrometheusService(PrometheusClient client) {
        this.client = client;
    }

    public PrometheusResponse getPrometheusResponse(int step, Instant start, Instant end) {
        return this.client.query(
            PROMETHEUS_QUERY,
            step,
            start.getEpochSecond(),
            end.getEpochSecond()
        );
    }
}
