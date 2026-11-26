package tech.illuin.wombat.core.asset.profile.inference;

import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.AssetProfile;
import tech.illuin.wombat.core.asset.profile.Lifetime;

/**
 * A server profile to infer from the cluster, with the fields the user chose to set themselves.
 */
public interface InferableServerProfile extends AssetProfile
{
    /**
     * @return the country hosting the nodes, null for the world
     */
    Country location();

    /**
     * @return the expected operational lifespan of the hardware, null for {@link KubernetesProfileInference#DEFAULT_LIFESPAN}
     */
    Lifetime lifespan();
}
