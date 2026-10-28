package tech.illuin.wombat.module.llm_google.connector.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleQueryResponse(
    @JsonProperty("timeSeries") List<TimeSeries> timeSeries,
    @JsonProperty("nextPageToken") String nextPageToken
) {
    public double sum()
    {
        if (this.timeSeries == null)
            return 0.0d;

        return this.timeSeries.stream()
            .filter(ts -> ts.points() != null)
            .flatMap(ts -> ts.points().stream())
            .mapToDouble(Point::numericValue)
            .sum();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TimeSeries(
        @JsonProperty("metric") JsonNode metric,
        @JsonProperty("resource") JsonNode resource,
        @JsonProperty("metricKind") String metricKind,
        @JsonProperty("valueType") String valueType,
        @JsonProperty("points") List<Point> points
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Point(
        @JsonProperty("interval") JsonNode interval,
        @JsonProperty("value") JsonNode value
    ) {
        public double numericValue()
        {
            if (this.value == null)
                return 0.0d;
            if (this.value.hasNonNull("int64Value"))
                return Double.parseDouble(this.value.get("int64Value").asText());
            if (this.value.hasNonNull("doubleValue"))
                return this.value.get("doubleValue").asDouble();
            return 0.0d;
        }
    }
}
