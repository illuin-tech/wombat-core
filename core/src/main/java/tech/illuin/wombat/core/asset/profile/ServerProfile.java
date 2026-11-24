package tech.illuin.wombat.core.asset.profile;

import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.activity.commons.Country;

public sealed interface ServerProfile extends AssetProfile permits CloudServerProfile, BareServerProfile
{
    @NotNull
    Country location();

    @NotNull
    Lifetime lifespan();

    default int nodeCount()
    {
        return 1;
    }
}
