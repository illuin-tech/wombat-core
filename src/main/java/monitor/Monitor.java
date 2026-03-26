package monitor;

import io.quarkus.scheduler.Scheduled;
import kubernetes.KubernetesResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Monitor {

    private final KubernetesResourceService kubernetesResourceService;
    private final MonitorProperties properties;


    private static final Logger logger = LoggerFactory.getLogger(Monitor.class);

    public Monitor(KubernetesResourceService kubernetesResourceService, MonitorProperties properties) {
        this.kubernetesResourceService = kubernetesResourceService;
        this.properties = properties;
    }

    @Scheduled(cron = "${monitor.cron}")
    public void monitor()
    {
        this.kubernetesResourceService.listPods(this.properties.targetNamespace());
    }
}
