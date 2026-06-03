package tech.illuin.wombat.k8s;

import tech.illuin.wombat.monitor.MonitoredResourceHandler;

import java.time.Instant;
import java.util.List;

public class K8SResourceHandler implements MonitoredResourceHandler<List<K8SProperties.ClusterProperties>>
{
    private final K8SResourceService k8SResourceService;

    public K8SResourceHandler(K8SResourceService k8SResourceService)
    {
        this.k8SResourceService = k8SResourceService;
    }

    @Override
    public void handle(Instant instant, List<K8SProperties.ClusterProperties> clusterConfigs)
    {
        this.k8SResourceService.listPods(instant, clusterConfigs);
    }
}
