package tech.illuin.wombat.persistence;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.metrics.MetricRecorderService;
import tech.illuin.wombat.persistence.backend.PersistenceBackend;
import tech.illuin.wombat.persistence.backend.PersistenceInitializer;

@ApplicationScoped
public class PersistenceConfig
{
    private static final int PRIORITY_INIT = 900;
    private static final int PRIORITY_SETUP = 1000;

    private static final Logger logger = LoggerFactory.getLogger(PersistenceConfig.class);

    /**
     * This hook is used for anything that needs to be performed before wiring the datasource and the backend object.
     *
     * @param event
     * @param initializer
     */
    void onStartInitialize(
        @Observes @Priority(PRIORITY_INIT) StartupEvent event,
        PersistenceInitializer initializer
    ) {
        logger.info("Running persistence initialize stage with initializer {}", initializer.getClass().getSimpleName());
        initializer.initialize();
    }

    /**
     * This hook is used for anything that needs to be performed after wiring the backend object.
     *
     * @param event
     * @param backend
     */
    void onStartSetup(
        @Observes @Priority(PRIORITY_SETUP) StartupEvent event,
        PersistenceBackend backend
    ) {
        logger.info("Running persistence setup stage with backend {}", backend.getClass().getSimpleName());
        backend.setup();
    }

    @Singleton
    @IfBuildProperty(name = "persistence.enable-kubernetes-metrics-persister", stringValue = "true")
    public KubernetesMetricsPersister provideKubernetesMetricsPersister(ServerMetricRepository repository, MetricRecorderService recorder)
    {
        return new KubernetesMetricsPersister(repository, recorder);
    }
}
