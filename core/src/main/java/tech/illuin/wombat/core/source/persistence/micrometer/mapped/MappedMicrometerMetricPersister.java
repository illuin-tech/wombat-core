package tech.illuin.wombat.core.source.persistence.micrometer.mapped;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.core.source.data.MetricData;
import tech.illuin.wombat.core.source.persistence.WombatMetricPersister;
import tech.illuin.wombat.core.source.persistence.WombatPersistenceException;

import java.util.Collection;
import java.util.List;

public class MappedMicrometerMetricPersister implements WombatMetricPersister
{
    private final MeterRegistry registry;
    private final Mapper mapper;

    private static final Logger logger = LoggerFactory.getLogger(MappedMicrometerMetricPersister.class);

    public MappedMicrometerMetricPersister(MeterRegistry registry, Mapper mapper)
    {
        this.registry = registry;
        this.mapper = mapper;
    }

    @Override
    public void persist(Collection<MetricData> metrics) throws WombatPersistenceException
    {
        logger.trace("Recording {} metrics points", metrics.size());
        for (MetricData data : metrics)
        {
            logger.trace("Current metrics {}", data);
            List<Tag> tags = this.mapper.mapTags(data);
            for (Value value : this.mapper.mapValues(data))
                this.record(value, tags);
        }
    }

    private void record(Value value, Collection<Tag> tags)
    {
        DistributionSummary.builder(value.key())
            .tags(tags)
            .register(this.registry)
            .record(value.value());
    }

    public interface Mapper
    {
        List<Tag> mapTags(MetricData data);

        List<Value> mapValues(MetricData data);
    }

    public record Value(
        String key,
        double value
    ) {}
}
