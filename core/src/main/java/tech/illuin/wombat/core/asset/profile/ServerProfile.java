package tech.illuin.wombat.core.asset.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public interface ServerProfile extends AssetProfile
{
    @NotNull
    ServerProvider provider();

    @NotBlank
    String instanceType();

    @NotBlank
    String location();

    @Positive
    int lifespan();
}
