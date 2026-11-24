package tech.illuin.wombat.module.kubernetes_metrics;

import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.asset.type.FamilyType;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.module.ProcessorFactory;
import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.module.kubernetes_metrics.activity.KubernetesActivityResolver;

import java.util.Optional;

/**
 * Resolves the activity of the Kubernetes assets whose metrics are sourced, from the metric store they were persisted to.
 */
public class KubernetesMetricsModule implements WombatModule
{
    public static final FamilyType TYPE = FamilyType.of("tech.illuin", "wombat-module", "kubernetes-metrics", ServiceFamily.KUBERNETES_CONTAINER);

    @Override
    public FamilyType type()
    {
        return TYPE;
    }

    @Override
    public Optional<ProcessorFactory<WombatActivityResolver>> activityResolver()
    {
        return Optional.of((services, asset) -> new KubernetesActivityResolver(services.metrics().kubernetes()));
    }
}
