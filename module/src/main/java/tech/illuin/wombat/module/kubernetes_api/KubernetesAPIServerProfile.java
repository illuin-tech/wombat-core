package tech.illuin.wombat.module.kubernetes_api;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.AssetProfile;
import tech.illuin.wombat.core.asset.profile.Lifetime;

@JsonClassDescription("Hardware of the cluster's nodes, a cloud instance type or a bare server specification")
// Profiles without a type are cloud ones, as all were before bare servers were supported
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", defaultImpl = KubernetesAPICloudProfile.class)
@JsonSubTypes({
    @JsonSubTypes.Type(value = KubernetesAPICloudProfile.class, name = "cloud"),
    @JsonSubTypes.Type(value = KubernetesAPIBareServerProfile.class, name = "server")
})
public sealed interface KubernetesAPIServerProfile extends AssetProfile permits KubernetesAPICloudProfile, KubernetesAPIBareServerProfile
{
    Country location();

    Lifetime lifespan();
}
