package tech.illuin.wombat.monitor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import tech.illuin.wombat.kubernetes.KubernetesResourceService;

@ApplicationScoped
public class MonitorConfig
{
    @Singleton
    public KubernetesResourceHandler provideKubernetesResourceHandler(KubernetesResourceService kubernetesResourceService)
    {
        return new KubernetesResourceHandler(kubernetesResourceService);
    }

    @Singleton
    public Monitor provideMonitor(KubernetesResourceHandler kubernetesResourceHandler, MonitorProperties properties)
    {
        return new Monitor(kubernetesResourceHandler, properties);
    }
}
