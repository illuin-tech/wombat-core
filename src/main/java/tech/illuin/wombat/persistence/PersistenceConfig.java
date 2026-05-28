package tech.illuin.wombat.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import tech.illuin.wombat.prometheus.PrometheusService;

@ApplicationScoped
public class PersistenceConfig {
    // DO NOT USE FOR NOW
    @Singleton
    @IfBuildProperty(name = "persistence.enable-meter-target", stringValue = "true")
    public MeterLoadTarget provideMeterLoadTarget(MeterRegistry registry, PrometheusService prometheusService){
        return new MeterLoadTarget(registry, prometheusService);
    }

    @Singleton
    @IfBuildProperty(name = "persistence.enable-memory-target", stringValue = "true")
    public MemoryLoadTarget provideMemoryLoadTarget(DatapointRepository repository, ObjectMapper mapper) {
        return new MemoryLoadTarget(repository, mapper);
    }
}
