package tech.illuin.wombat.module.llm_prometheus.connector.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Optional;

public record PrometheusQueryResponse(
    @JsonProperty("status") String status,
    @JsonProperty("data") Data data
) {
    public Optional<Double> firstValue()
    {
        if (this.data == null || this.data.result() == null || this.data.result().isEmpty())
            return Optional.empty();

        List<JsonNode> value = this.data.result().getFirst().value();
        if (value == null || value.size() < 2 || value.get(1) == null)
            return Optional.empty();
        try {
            return Optional.of(Double.parseDouble(value.get(1).asText()));
        }
        catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public record Data(
        @JsonProperty("resultType") String resultType,
        @JsonProperty("result") List<Result> result
    ) {}

    public record Result(
        @JsonProperty("metric") JsonNode metric,
        @JsonProperty("value") List<JsonNode> value
    ) {}
}
