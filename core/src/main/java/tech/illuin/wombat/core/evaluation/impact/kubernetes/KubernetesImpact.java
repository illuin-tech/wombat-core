package tech.illuin.wombat.core.evaluation.impact.kubernetes;

import tech.illuin.wombat.core.asset.AssetType;
import tech.illuin.wombat.core.asset.ServiceFamily;
import tech.illuin.wombat.core.asset.profile.ServerProfile;
import tech.illuin.wombat.core.evaluation.impact.commons.Footprint;
import tech.illuin.wombat.core.evaluation.impact.commons.ServiceImpact;

public record KubernetesImpact(
    String serviceId,
    AssetType assetType,
    ServerProfile profile,
    double share,
    Footprint footprint,
    ClusterInfo location,
    ServerSpecification serverSpecification,
    double loadPercent,
    int nodeCount
) implements ServiceImpact
{
    public ServiceFamily profileFamily()
    {
        return ServiceFamily.KUBERNETES_CONTAINER;
    }
}
