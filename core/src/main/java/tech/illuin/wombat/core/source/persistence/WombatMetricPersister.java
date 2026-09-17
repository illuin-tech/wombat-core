package tech.illuin.wombat.core.source.persistence;

import tech.illuin.wombat.core.source.data.MetricData;

import java.util.Collection;

public interface WombatMetricPersister
{
    void persist(Collection<MetricData> metrics) throws WombatPersistenceException;
}
