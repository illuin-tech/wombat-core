package tech.illuin.wombat.metrics;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.step.StepRegistryConfig;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.persistence.ServerMetricRepository;

import java.time.Duration;
import java.util.concurrent.Executors;

@ApplicationScoped
public class MetricsConfig
{

    private static final Logger logger = LoggerFactory.getLogger(MetricsConfig.class);

    @Singleton
    public SqliteStepMeterRegistry provideStepRegistry(
        MetricsProperties properties,
        ServerMetricRepository repository
    )
    {
        Duration window = properties.aggregationWindow().asDuration();
        StepRegistryConfig stepConfig = new StepRegistryConfig()
        {
            @Override
            public String prefix()
            {
                return "wombat";
            }

            @Override
            public Duration step()
            {
                return window;
            }

            @Override
            public String get(@NonNull String key)
            {
                return null;
            }
        };
        return new SqliteStepMeterRegistry(stepConfig, Clock.SYSTEM, repository);
    }

    @Singleton
    public MetricRecorderService provideMetricRecorder(SqliteStepMeterRegistry registry)
    {
        return new MetricRecorderService(registry);
    }

    void onStart(
        @Observes StartupEvent event,
        MeterRegistry rootRegistry,
        SqliteStepMeterRegistry stepRegistry,
        MetricsProperties properties
    )
    {
        logger.info("Root MeterRegistry implementation: {}", rootRegistry.getClass().getName());
        stepRegistry.start(Executors.defaultThreadFactory());
        if (rootRegistry instanceof CompositeMeterRegistry composite)
        {
            composite.add(stepRegistry);
            logger.info("Attached SqliteStepMeterRegistry to CompositeMeterRegistry (step={})", properties.aggregationWindow().asDuration());
        }
        else {
            logger.info("Root registry is not composite ({}); SqliteStepMeterRegistry runs standalone (step={})",
                rootRegistry.getClass().getSimpleName(), properties.aggregationWindow().asDuration());
        }
    }

    void onStop(@Observes ShutdownEvent event, SqliteStepMeterRegistry stepRegistry)
    {
        stepRegistry.close();
    }
}
