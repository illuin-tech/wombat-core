package tech.illuin.wombat.core.asset.profile;

import jakarta.validation.constraints.NotBlank;

public interface Profile
{
    @NotBlank
    String id();
}
