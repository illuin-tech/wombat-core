package tech.illuin.wombat.core.source.persistence;

import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.source.data.MetricData;

import java.util.Collection;

public interface WombatMetricPersister
{
    void persist(AssetIdentity identity, AssetType type, Collection<MetricData> metrics) throws WombatPersistenceException;
}
