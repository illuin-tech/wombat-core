package persistence;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import prometheus.PrometheusService;

@ApplicationScoped
public class PersistenceConfig {
    @Singleton
    @IfBuildProperty(name = "persistence.enable-meter-target", stringValue = "true")
    public MeterLoadTarget provideMeterLoadTarget(MeterRegistry registry, PrometheusService prometheusService){
        return new MeterLoadTarget(registry, prometheusService);
    }

    @Singleton
    @IfBuildProperty(name = "persistence.enable-memory-target", stringValue = "true")
    public MemoryLoadTarget provideMemoryLoadTarget()
    {
        return new MemoryLoadTarget();
    }
}
