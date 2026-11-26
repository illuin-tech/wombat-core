package tech.illuin.wombat.module.kubernetes_api;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.Lifetime;
import tech.illuin.wombat.module.kubernetes_api.profile.InferableServerProfile;

@JsonClassDescription(
    "Hardware of the cluster's nodes, inferred from its API: the nodes' instance type or capacity (needs to list nodes), "
    + "else the polled namespaces' resource quotas, else their containers' resources (need to list resourcequotas or pods in them)"
)
public record KubernetesAPIInferredProfile(
    @JsonPropertyDescription("Country hosting the nodes, the world when omitted") Country location,
    @JsonPropertyDescription("Expected operational lifespan of the hardware, 5 years when omitted") Lifetime lifespan
) implements KubernetesAPIServerProfile, InferableServerProfile
{
    public static final KubernetesAPIInferredProfile DEFAULT = new KubernetesAPIInferredProfile(null, null);
}
