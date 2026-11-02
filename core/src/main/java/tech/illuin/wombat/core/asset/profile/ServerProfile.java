package tech.illuin.wombat.core.asset.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.activity.commons.Country;

public interface ServerProfile extends AssetProfile
{
    @NotNull
    ServerProvider provider();

    @NotBlank
    String instanceType();

    @NotNull
    Country location();

    @NotNull
    Lifetime lifespan();

    default int nodeCount()
    {
        return 1;
    }
}
