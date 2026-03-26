package kubernetes;

import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.inject.Instance;
import persistence.LoadTarget;

import java.time.Instant;
import java.util.Set;

public class KubernetesResourceService {


    private final KubernetesClient kubernetesClient;
    private final Instance<LoadTarget> targets;

    public KubernetesResourceService(KubernetesClient kubernetesClient, Instance<LoadTarget> targets) {
        this.kubernetesClient = kubernetesClient;
        this.targets = targets;
    }

    public void listPods(String namespace) {
        Instant instant = Instant.now();
        this.kubernetesClient.top()
            .pods()
            .inNamespace(namespace)
            .metrics()
            .getItems()
            .forEach(podMetrics -> this.targets
                .forEach(target -> target.outputToTarget(instant, podMetrics, namespace))
            );
    }
}
