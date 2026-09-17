package tech.illuin.wombat.core.source.persistence.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import tech.illuin.wombat.core.asset.ServiceFamily;
import tech.illuin.wombat.core.source.data.MetricData;
import tech.illuin.wombat.core.source.persistence.CompositeMetricPersister;
import tech.illuin.wombat.core.source.persistence.WombatMetricPersister;
import tech.illuin.wombat.core.source.persistence.WombatPersistenceException;
import tech.illuin.wombat.core.source.persistence.micrometer.mapped.KubernetesMapper;
import tech.illuin.wombat.core.source.persistence.micrometer.mapped.LLMMapper;
import tech.illuin.wombat.core.source.persistence.micrometer.mapped.MappedMicrometerMetricPersister;

import java.util.Collection;

public class MicrometerMetricPersister implements WombatMetricPersister
{
    private final CompositeMetricPersister composite;

    public MicrometerMetricPersister(MeterRegistry registry)
    {
        this.composite = new CompositeMetricPersister()
            .register(ServiceFamily.KUBERNETES_CONTAINER, new MappedMicrometerMetricPersister(registry, new KubernetesMapper()))
            .register(ServiceFamily.LLM, new MappedMicrometerMetricPersister(registry, new LLMMapper()))
        ;
    }

    @Override
    public void persist(Collection<MetricData> metrics) throws WombatPersistenceException
    {
        this.composite.persist(metrics);
    }
}
