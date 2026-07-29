package tech.illuin.wombat.prometheus.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.OptionalDouble;

public record PrometheusQueryResponse(
    @JsonProperty("status") String status,
    @JsonProperty("data") Data data
)
{
    public OptionalDouble firstValue()
    {
        if (this.data == null || this.data.result() == null || this.data.result().isEmpty())
            return OptionalDouble.empty();
        List<JsonNode> value = this.data.result().getFirst().value();
        if (value == null || value.size() < 2 || value.get(1) == null)
            return OptionalDouble.empty();
        try
        {
            return OptionalDouble.of(Double.parseDouble(value.get(1).asText()));
        }
        catch (NumberFormatException e) {
            return OptionalDouble.empty();
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
