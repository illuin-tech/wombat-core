package tech.illuin.wombat.core.source;

import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.module.AssetProcessor;
import tech.illuin.wombat.core.source.data.MetricData;

import java.time.Instant;
import java.util.List;

public interface WombatSource extends AssetProcessor
{
    List<MetricData> source(Instant heartbeat, Asset asset) throws WombatSourceException;
}
