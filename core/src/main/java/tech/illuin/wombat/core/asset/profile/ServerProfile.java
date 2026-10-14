package tech.illuin.wombat.core.asset.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import tech.illuin.wombat.core.activity.commons.Country;

public interface ServerProfile extends AssetProfile
{
    @NotNull
    ServerProvider provider();

    @NotBlank
    String instanceType();

    @NotNull
    Country location();

    @Positive
    int lifespan();
}
