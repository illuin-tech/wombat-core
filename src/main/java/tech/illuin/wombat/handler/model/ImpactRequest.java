package tech.illuin.wombat.handler.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.persistence.model.TimeRange;

import java.util.List;

public record ImpactRequest(
    @JsonProperty("source_time_range") TimeRange sourceTimeRange,
    @JsonProperty("configs") List<ProviderConfig> configs
)
{
}
