package tech.illuin.wombat.core.asset.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface LLMProfile extends AssetProfile
{
    @Override
    default String id()
    {
        return this.model();
    }

    @NotNull
    LLMProvider provider();

    @NotBlank
    String model();

    @NotBlank
    String location();
}
