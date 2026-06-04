package tech.illuin.wombat.persistence;

import tech.illuin.wombat.persistence.model.ContainerLocation;

import java.util.List;
import java.util.Map;

public record LoadData(
    double cpuUsage,
    Map<String, Double> containerShares,
    Map<String, List<ContainerLocation>> containerLocations
)
{
}
