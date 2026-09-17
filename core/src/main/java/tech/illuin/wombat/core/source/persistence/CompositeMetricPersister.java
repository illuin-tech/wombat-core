package tech.illuin.wombat.core.source.persistence;

import tech.illuin.wombat.core.asset.ServiceFamily;
import tech.illuin.wombat.core.source.data.MetricData;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CompositeMetricPersister implements WombatMetricPersister
{
    private final Map<ServiceFamily, WombatMetricPersister> persisters;

    public CompositeMetricPersister()
    {
        this.persisters = new HashMap<>();
    }

    public CompositeMetricPersister register(ServiceFamily family, WombatMetricPersister persister)
    {
        this.persisters.put(family, persister);
        return this;
    }

    @Override
    public void persist(Collection<MetricData> metrics) throws WombatPersistenceException
    {
        for (ServiceFamily family : this.persisters.keySet())
            this.persist(family, metrics.stream().filter(m -> m.serviceFamily() == family).toList());
    }

    private void persist(ServiceFamily family, Collection<MetricData> metrics) throws WombatPersistenceException
    {
        this.persisters.get(family).persist(metrics);
    }
}
