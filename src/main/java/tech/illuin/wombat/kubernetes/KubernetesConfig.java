package tech.illuin.wombat.kubernetes;

import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;
import tech.illuin.wombat.persistence.LoadTarget;

@ApplicationScoped
public class KubernetesConfig
{
    @Singleton
    public KubernetesResourceService provideKubernetesResourceService(Instance<LoadTarget> targets)
    {
        return new KubernetesResourceService(new KubernetesClientBuilder().build(), targets);
    }
}
