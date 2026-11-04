package tech.illuin.wombat.module.llm_vertexai.connector.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleQueryResponse(
    @JsonProperty("timeSeries") List<TimeSeries> timeSeries,
    @JsonProperty("nextPageToken") String nextPageToken
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TimeSeries(
        @JsonProperty("resource") Resource resource,
        @JsonProperty("points") List<Point> points
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Resource(@JsonProperty("labels") Labels labels) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Labels(
        @JsonProperty("model_user_id") String model,
        @JsonProperty("model_version_id") String modelVersionId,
        @JsonProperty("project_id") String projectId,
        @JsonProperty("location") String location,
        @JsonProperty("publisher") String publisher
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

        public Optional<Instant> endTime()
        {
            if (this.interval == null || !this.interval.hasNonNull("endTime"))
                return Optional.empty();
            return Optional.of(Instant.parse(this.interval.get("endTime").asText()));
        }
    }
}
