package tech.illuin.vigilantwombat.kubernetes;

import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;
import tech.illuin.vigilantwombat.persistence.LoadTarget;

@ApplicationScoped
public class KubernetesConfig {
    @Singleton
    public KubernetesResourceService provideKubernetesResourceService(Instance<LoadTarget> targets)
    {
        return new KubernetesResourceService(new KubernetesClientBuilder().build(), targets);
    }
}
