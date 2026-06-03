package tech.illuin.wombat.monitor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import tech.illuin.wombat.k8s.K8SResourceHandler;
import tech.illuin.wombat.k8s.K8SResourceService;

@ApplicationScoped
public class MonitorConfig
{
    @Singleton
    public K8SResourceHandler provideKubernetesResourceHandler(K8SResourceService k8sResourceService)
    {
        return new K8SResourceHandler(k8sResourceService);
    }

    @Singleton
    public Monitor provideMonitor(K8SResourceHandler k8sResourceHandler, MonitorProperties properties)
    {
        return new Monitor(k8sResourceHandler, properties);
    }
}
