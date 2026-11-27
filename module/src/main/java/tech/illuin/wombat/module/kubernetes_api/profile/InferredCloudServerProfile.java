package tech.illuin.wombat.module.kubernetes_api.profile;

import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.CloudServerProfile;
import tech.illuin.wombat.core.asset.profile.InferredProfile;
import tech.illuin.wombat.core.asset.profile.Lifetime;
import tech.illuin.wombat.core.asset.profile.ProfileOrigin;
import tech.illuin.wombat.core.asset.profile.ServerProvider;

import java.util.List;

/**
 * @param nodeCount the number of nodes of this instance type
 */
public record InferredCloudServerProfile(
    ServerProvider provider,
    String instanceType,
    Country location,
    Lifetime lifespan,
    int nodeCount,
    ProfileOrigin origin,
    List<String> warnings
) implements CloudServerProfile, InferredProfile
{
    public InferredCloudServerProfile
    {
        warnings = warnings != null ? List.copyOf(warnings) : List.of();
    }
}
