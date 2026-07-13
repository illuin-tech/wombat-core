package tech.illuin.wombat.handler.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.monitor.AssetProperties;
import tech.illuin.wombat.monitor.AssetType;
import tech.illuin.wombat.monitor.Environment;
import tech.illuin.wombat.persistence.model.TimeRange;

import java.util.List;

public record EnvironmentConfig(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("assets") List<AssetSummary> assets,
    @JsonProperty("time_range") TimeRange timeRange
)
{
    public static EnvironmentConfig from(String id, Environment environment)
    {
        return from(id, environment, null);
    }

    public static EnvironmentConfig from(String id, Environment environment, TimeRange timeRange)
    {
        List<AssetSummary> assets = environment.assets().stream().map(AssetSummary::from).toList();
        return new EnvironmentConfig(id, environment.name(), assets, timeRange);
    }

    public record AssetSummary(
        String id,
        String name,
        AssetType type
    )
    {
        public static AssetSummary from(AssetProperties properties)
        {
            return new AssetSummary(properties.id(), properties.name(), properties.type());
        }
    }
}
