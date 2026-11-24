package tech.illuin.wombat.core.asset.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public non-sealed interface CloudServerProfile extends ServerProfile
{
    @NotNull
    ServerProvider provider();

    @NotBlank
    String instanceType();
}
