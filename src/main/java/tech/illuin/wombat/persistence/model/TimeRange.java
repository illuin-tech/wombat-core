package tech.illuin.wombat.persistence.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.smallrye.common.constraint.NotNull;

import java.time.Instant;

public record TimeRange(
    @NotNull @JsonProperty("start") Instant start,
    @NotNull @JsonProperty("end") Instant end
) {}
