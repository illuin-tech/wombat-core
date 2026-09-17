package tech.illuin.wombat.core.evaluation.impact.kubernetes;

import tech.illuin.wombat.core.activity.kubernetes.KubernetesActivityData;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.evaluation.impact.commons.AssetImpact;
import tech.illuin.wombat.core.asset.profile.ServerProfile;

public interface KubernetesLoadImpactResolver
{
    AssetImpact resolve(KubernetesActivityData load, Asset asset, ServerProfile profile);
}
