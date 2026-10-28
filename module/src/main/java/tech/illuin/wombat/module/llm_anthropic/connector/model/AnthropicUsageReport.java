package tech.illuin.wombat.module.llm_anthropic.connector.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AnthropicUsageReport(
    @JsonProperty("data") List<Bucket> data,
    @JsonProperty("has_more") boolean hasMore,
    @JsonProperty("next_page") String nextPage
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Bucket(
        @JsonProperty("starting_at") String startingAt,
        @JsonProperty("ending_at") String endingAt,
        @JsonProperty("results") List<Result> results
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
        @JsonProperty("model") String model,
        @JsonProperty("uncached_input_tokens") long uncachedInputTokens,
        @JsonProperty("cache_read_input_tokens") long cacheReadInputTokens,
        @JsonProperty("output_tokens") long outputTokens
    ) {}
}
