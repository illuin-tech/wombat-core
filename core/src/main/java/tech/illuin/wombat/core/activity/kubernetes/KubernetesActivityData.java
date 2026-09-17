package tech.illuin.wombat.core.activity.kubernetes;

import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.ActivityRegime;
import tech.illuin.wombat.core.asset.ServiceFamily;
import tech.illuin.wombat.core.evaluation.impact.kubernetes.ClusterInfo;

import java.util.Map;
import java.util.Set;

public record KubernetesActivityData(
    ActivityRegime regime,
    Set<String> serviceIds,
    TimeRange range,
    double cpuUsage,
    Map<String, Double> containerShares,
    Map<String, ClusterInfo> containerLocations
) implements ActivityData
{
    @Override
    public ServiceFamily family() {
        return ServiceFamily.KUBERNETES_CONTAINER;
    }
}
