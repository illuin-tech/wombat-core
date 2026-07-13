package tech.illuin.wombat.handler.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.persistence.model.TimeRange;

import java.util.List;

public record ImpactRequest(
    @JsonProperty("source_time_range") TimeRange sourceTimeRange,
    @JsonProperty("environment_id") String environmentId,
    @JsonProperty("asset_ids") List<String> assetIds
)
{
    public ImpactRequest(TimeRange sourceTimeRange, List<String> assetIds)
    {
        this(sourceTimeRange, null, assetIds);
    }
}
