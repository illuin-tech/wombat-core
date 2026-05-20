package tech.illuin.vigilantwombat.monitor;

import tech.illuin.vigilantwombat.kubernetes.KubernetesResourceService;

public class KubernetesResourceHandler implements MonitoredResourceHandler<MonitoredKubernetesNamespace> {
    private final KubernetesResourceService kubernetesResourceService;

    public KubernetesResourceHandler(KubernetesResourceService kubernetesResourceService) {
        this.kubernetesResourceService = kubernetesResourceService;
    }

    @Override
    public void handle(String name, MonitoredKubernetesNamespace config) {
        this.kubernetesResourceService.listPods(config.namespace());
    }
}
