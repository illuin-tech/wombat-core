package tech.illuin.vigilantwombat.monitor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import tech.illuin.vigilantwombat.kubernetes.KubernetesResourceService;

@ApplicationScoped
public class MonitorConfig {
    @Singleton
    public Monitor provideMonitor(KubernetesResourceService kubernetesResourceService, MonitorProperties properties)
    {
        return new Monitor(kubernetesResourceService, properties);
    }
}
