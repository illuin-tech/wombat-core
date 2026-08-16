package tech.illuin.wombat.persistence;

import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import tech.illuin.wombat.metrics.MetricRecorderService;

@ApplicationScoped
public class PersistenceConfig
{
    @Singleton
    @IfBuildProperty(name = "persistence.enable-kubernetes-metrics-persister", stringValue = "true")
    public KubernetesMetricsPersister provideKubernetesMetricsPersister(ServerMetricRepository repository, MetricRecorderService recorder)
    {
        return new KubernetesMetricsPersister(repository, recorder);
    }
}
